
from distutils.log import debug
import boto3
import botocore
import os
import requests
import time
import json
import configparser
import re
from dateutil.parser import parse
import datetime


########################################
# Constants
########################################
with open('horizontal-scaling-config.json') as file:
    configuration = json.load(file)

LOAD_GENERATOR_AMI = configuration['load_generator_ami']
WEB_SERVICE_AMI = configuration['web_service_ami']
INSTANCE_TYPE = configuration['instance_type']

# Credentials fetched from environment variables
SUBMISSION_USERNAME = os.environ['SUBMISSION_USERNAME']
SUBMISSION_PASSWORD = os.environ['SUBMISSION_PASSWORD']

########################################
# Tags
########################################
tag_pairs = [
    ("Project", "vm-scaling"),
]
TAGS = [{'Key': k, 'Value': v} for k, v in tag_pairs]

TEST_NAME_REGEX = r'name=(.*log)'

########################################
# Utility functions
########################################


def debug_response(res):
    with open("debug_response.json", "w") as f:
        json.dump(res, f)


def create_instance(ami, sg_name, ins_type='m5.large', tag='vm-scaling'):
    """
    Given AMI, create and return an AWS EC2 instance object
    :param ami: AMI image name to launch the instance with
    :param sg_name: name of the security group to be attached to instance
    :return: instance object
    """
    instance = None

    # TODO: Create an EC2 instance
    # Wait for the instance to enter the running state
    # Return the instance attributes

    ec2_client = boto3.resource('ec2', region_name='us-east-1')
    instances = ec2_client.create_instances(
        ImageId=ami,
        InstanceType=ins_type,
        MaxCount=1,
        MinCount=1,
        Monitoring={
            'Enabled': True
        },
        SecurityGroupIds=[
            sg_name,
        ],
        TagSpecifications=[
            {
                "ResourceType": 'instance',
                "Tags": [
                    {
                        'Key': 'Project',
                        'Value': tag
                    },
                ]
            }
        ],
    )
    instance = instances[0]
    print("Instance ID:", instance)
    # Wait here
    instance.wait_until_running()
    # Refresh
    instance.load()
    # print(instance.public_dns_name)
    return instance

def terminate_all_instances(target_ami:list=[], target_id:list=[]):
    ec2_res = boto3.resource('ec2', region_name='us-east-1')
    target_instances = ec2_res.instances.filter(
        Filters=[{'Name': 'instance-state-name', 'Values': ['running','stopped']}])
    inst_list = []
    for instance in target_instances:
        if instance.image_id in target_ami or instance.id in target_id:
            print("To be terminated:", instance.id, instance.instance_type)
            inst_list.append(instance.id)
    try:
        ec2_client = boto3.client('ec2', region_name='us-east-1')
        response = ec2_client.terminate_instances(
                    InstanceIds=inst_list
                    )
        waiter = ec2_client.get_waiter('instance_terminated')
        waiter.wait(InstanceIds=inst_list)
    except: print("Did find anything to terminate.")

def initialize_test(lg_dns, first_web_service_dns):
    """
    Start the horizontal scaling test
    :param lg_dns: Load Generator DNS
    :param first_web_service_dns: Web service DNS
    :return: Log file name
    """
    add_ws_string = 'http://{lg_dns}/test/horizontal?dns={first_web_service_dns}'.format(
        lg_dns=lg_dns, first_web_service_dns=first_web_service_dns
    )
    print("Test string", add_ws_string)
    response = None
    while not response or response.status_code != 200:
        try:
            response = requests.get(add_ws_string)
        except requests.exceptions.ConnectionError:
            time.sleep(1)
            pass
    # TODO: return log File name
    return get_test_id(response)

def print_section(msg):
    """
    Print a section separator including given message
    :param msg: message
    :return: None
    """
    print(('#' * 40) + '\n# ' + msg + '\n' + ('#' * 40))

def get_test_id(response):
    """
    Extracts the test id from the server response.
    :param response: the server response.
    :return: the test name (log file name).
    """
    response_text = response.text

    regexpr = re.compile(TEST_NAME_REGEX)

    return regexpr.findall(response_text)[0]

def is_test_complete(lg_dns, log_name):
    """
    Check if the horizontal scaling test has finished
    :param lg_dns: load generator DNS
    :param log_name: name of the log file
    :return: True if Horizontal Scaling test is complete and False otherwise.
    """

    log_string = 'http://{}/log?name={}'.format(lg_dns, log_name)

    # creates a log file for submission and monitoring
    f = open(log_name + ".log", "w")
    log_text = requests.get(log_string).text
    f.write(log_text)
    f.close()
    return '[Test finished]' in log_text

def add_web_service_instance(lg_dns, sg2_name, log_name):
    """
    Launch a new WS (Web Server) instance and add to the test
    :param lg_dns: load generator DNS
    :param sg2_name: name of WS security group
    :param log_name: name of the log file
    """
    ins = create_instance(WEB_SERVICE_AMI, sg2_name)
    print("New WS launched. id={}, dns={}".format(
        ins.instance_id,
        ins.public_dns_name)
    )
    add_req = 'http://{}/test/horizontal/add?dns={}'.format(
        lg_dns,
        ins.public_dns_name
    )
    while True:
        if requests.get(add_req).status_code == 200:
            print("New WS submitted to LG.")
            return ins
        elif is_test_complete(lg_dns, log_name):
            print("New WS not submitted because test already completed.")
            break    

def authenticate(lg_dns, submission_password, submission_username):
    """
    Authentication on LG
    :param lg_dns: LG DNS
    :param submission_password: SUBMISSION_PASSWORD
    :param submission_username: SUBMISSION_USERNAME
    :return: None
    """
    authenticate_string = 'http://{}/password?passwd={}&username={}'.format(
        lg_dns, submission_password, submission_username
    )
    response = None
    while not response or response.status_code != 200:
        try:
            response = requests.get(authenticate_string)
            break
        except requests.exceptions.ConnectionError:
            pass

def get_rps(lg_dns, log_name):
    """
    Return the current RPS as a floating point number
    :param lg_dns: LG DNS
    :param log_name: name of log file
    :return: latest RPS value
    """

    log_string = 'http://{}/log?name={}'.format(lg_dns, log_name)
    config = configparser.ConfigParser(strict=False)
    config.read_string(requests.get(log_string).text)
    sections = config.sections()
    sections.reverse()
    rps = 0
    for sec in sections:
        if 'Current rps=' in sec:
            rps = float(sec[len('Current rps='):])
            break
    return rps

def get_test_start_time(lg_dns, log_name):
    """
    Return the test start time in UTC
    :param lg_dns: LG DNS
    :param log_name: name of log file
    :return: datetime object of the start time in UTC (tcutz datetime)
    """
    log_string = 'http://{}/log?name={}'.format(lg_dns, log_name)
    start_time = None
    while start_time is None:
        config = configparser.ConfigParser(strict=False)
        config.read_string(requests.get(log_string).text)
        # By default, options names in a section are converted
        # to lower case by configparser
        start_time = dict(config.items('Test')).get('starttime', None)
    return parse(start_time)    

def create_security_groups(client, sg_permissions, vpc_id, name, description,tag='vm-scaling'):
    try:
        response = client.create_security_group(GroupName=name,
                                                Description=description,
                                                VpcId=vpc_id,
                                                TagSpecifications=[
                                                    {
                                                        "ResourceType": 'security-group',
                                                        "Tags": [
                                                            {
                                                                'Key': 'Project',
                                                                'Value': tag
                                                            },
                                                        ]
                                                    }
                                                ]
                                                )
        security_group_id = response['GroupId']
        data = client.authorize_security_group_ingress(
            GroupId=security_group_id,
            IpPermissions=sg_permissions)
        print('Ingress Successfully Set %s' % data)
    except botocore.exceptions.ClientError as e:
        response = client.delete_security_group(
            GroupName=name)
        security_group_id = create_security_groups(
            client, sg_permissions, vpc_id, name, description)
    return security_group_id

def get_metric_avg(ins_Id:str, starttime:datetime.datetime, seconds_interval:int=600, 
                    metric:str="CPUUtilization",):
    client = boto3.client(service_name='cloudwatch', region_name='us-east-1')

    response = client.get_metric_statistics(
        Namespace = 'AWS/EC2',
        Period = seconds_interval,
        StartTime = datetime.datetime.utcnow() - datetime.timedelta(seconds = seconds_interval),
        EndTime = datetime.datetime.utcnow(),
        MetricName = metric,
        Statistics=['Average'], Unit='Percent',
        Dimensions = [
            {'Name': 'InstanceId', 'Value': ins_Id}
        ])
    return response['Datapoints'][0]['Average']

########################################
# Main routine
########################################
def main():
    # BIG PICTURE TODO: Provision resources to achieve horizontal scalability
    #   - Create security groups for Load Generator and Web Service
    #   - Provision a Load Generator instance
    #   - Provision a Web Service instance
    #   - Register Web Service DNS with Load Generator
    #   - Add Web Service instances to Load Generator
    #   - Terminate resources
    try:
        terminate_all_instances(target_ami=[LOAD_GENERATOR_AMI, WEB_SERVICE_AMI])
        ec2_client = boto3.client('ec2', region_name='us-east-1')

        print_section('1 - create two security groups')
        sg_permissions = [
            {'IpProtocol': 'tcp',
            'FromPort': 80,
            'ToPort': 80,
            'IpRanges': [{'CidrIp': '0.0.0.0/0'}],
            'Ipv6Ranges': [{'CidrIpv6': '::/0'}],
            }
        ]
        # TODO: Create two separate security groups and obtain the group ids
        response = ec2_client.describe_vpcs()
        vpc_id = response.get('Vpcs', [{}])[0].get('VpcId', '')
        # Security group for Load Generator instances
        sg1_id = create_security_groups(
            ec2_client, sg_permissions, vpc_id, "Load Balancer", "In all-80-in Out all")
        # Security group for Web Service instances
        sg2_id = create_security_groups(
            ec2_client, sg_permissions, vpc_id, "Load Generator", "In all-80-in Out all")

        print_section('2 - create LG')
        # TODO: Create Load Generator instance and obtain ID and DNS
        lg = create_instance(LOAD_GENERATOR_AMI, sg1_id)
        lg_id = lg.instance_id
        lg_dns = lg.public_dns_name
        print("Load Generator running: id={} dns={}".format(lg_id, lg_dns))
        ######################################
        print_section('3. Authenticate with the load generator')
        authenticate(lg_dns, SUBMISSION_PASSWORD, SUBMISSION_USERNAME)
        # TODO: Create First Web Service Instance and obtain the DNS
        web_ins = create_instance(WEB_SERVICE_AMI, sg2_id)
        print("New WS launched. id={}, dns={}".format(
            web_ins.instance_id,
            web_ins.public_dns_name)
        )
        logger = initialize_test(lg_dns, web_ins.public_dns_name)
        
        print_section('4. Submit the first WS instance DNS to LG, starting test.')
        #Initialize parameters for load scaling
        time_interval = 5   # minutes
        all_web_instances = []
        all_rps = []
        while not is_test_complete(lg_dns, logger):
            # TODO: Check RPS and last launch time
            rps = get_rps(lg_dns, logger)
            all_rps.append(rps)
            # TODO: Add New Web Service Instance if Required
            # append average cpu util of all isntance
            last_launch_time = get_test_start_time(lg_dns, logger).timestamp()
            # Scale out
            avg_rps = sum(all_rps)/len(all_rps)
            if avg_rps <= 50 and \
                (datetime.datetime.now().timestamp()-last_launch_time)>=100:
                print("Current avg RPS:", avg_rps)
                web_ins = add_web_service_instance(
                    lg_dns=lg_dns, sg2_name=sg2_id, log_name=logger)
                all_web_instances.append(web_ins.instance_id)
                all_rps = []
            time.sleep(2)
          
        print_section('End Test')
        # TODO: Terminate Resources
        terminate_all_instances(target_ami=[LOAD_GENERATOR_AMI, WEB_SERVICE_AMI])
    except Exception as e:
        print(e)
        terminate_all_instances(target_ami=[LOAD_GENERATOR_AMI, WEB_SERVICE_AMI])

if __name__ == '__main__':
    print("Main:")

    main()
