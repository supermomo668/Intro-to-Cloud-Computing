
import boto3
import botocore
import os
import requests
import time
import json
import re

########################################
# Constants
########################################
with open('auto-scaling-config.json') as file:
    configuration = json.load(file)

LOAD_GENERATOR_AMI = configuration['load_generator_ami']
WEB_SERVICE_AMI = configuration['web_service_ami']
INSTANCE_TYPE = configuration['instance_type']
SUBMISSION_USERNAME = os.environ['SUBMISSION_USERNAME']
SUBMISSION_PASSWORD = os.environ['SUBMISSION_PASSWORD']

healthCheckPort = '80'
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


def terminate_all_instances(target_ami: list = [], target_id: list = []):
    ec2_res = boto3.resource('ec2', region_name='us-east-1')
    target_instances = ec2_res.instances.filter(
        Filters=[{'Name': 'instance-state-name', 'Values': ['running', 'stopped']}])
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
    except:
        print("Did find anything to terminate.")


def create_security_groups(client, sg_permissions, vpc_id, name, description, tag='vm-scaling'):
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
    except botocore.exceptions.ClientError as e:    # already exists
        response = client.describe_security_groups(
            Filters=[
                dict(Name='group-name', Values=[name])
                ]
                )
    security_group_id = response['SecurityGroups'][0]['GroupId']
    return security_group_id


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
                "Tags": TAGS
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


def initialize_test(load_generator_dns, first_web_service_dns):
    """
    Start the auto scaling test
    :param lg_dns: Load Generator DNS
    :param first_web_service_dns: Web service DNS
    :return: Log file name
    """

    add_ws_string = 'http://{}/autoscaling?dns={}'.format(
        load_generator_dns, first_web_service_dns
    )
    response = None
    while not response or response.status_code != 200:
        try:
            response = requests.get(add_ws_string)
        except requests.exceptions.ConnectionError:
            time.sleep(1)
            pass

    # TODO: return log File name
    return get_test_id(response)


def initialize_warmup(load_generator_dns, load_balancer_dns):
    """
    Start the warmup test
    :param lg_dns: Load Generator DNS
    :param load_balancer_dns: Load Balancer DNS
    :return: Log file name
    """

    add_ws_string = 'http://{}/warmup?dns={}'.format(
        load_generator_dns, load_balancer_dns
    )
    response = None
    while not response or response.status_code != 200:
        try:
            response = requests.get(add_ws_string)
        except requests.exceptions.ConnectionError:
            time.sleep(1)
            pass
    # TODO: return log File name
    return get_test_id(response)


def get_test_id(response):
    """
    Extracts the test id from the server response.
    :param response: the server response.
    :return: the test name (log file name).
    """
    response_text = response.text

    regexpr = re.compile(TEST_NAME_REGEX)

    return regexpr.findall(response_text)[0]


def destroy_resources(msg):
    """
    Delete all resources created for this task
    :param msg: message {'AutoScalingGroupName':str,
                        'LoadBalancerArn': str}
    :return: None
    """
    # TODO: implement this method
    # Delete instance and ASG
    asg_client = boto3.client('autoscaling', region_name='us-east-1')
    asg_client.update_auto_scaling_group(
        AutoScalingGroupName=msg['AutoScalingGroupName'],
        MaxSize=0, 
        DesiredCapacity=0
    )
    del_scale_re = asg_client.delete_auto_scaling_group(
                AutoScalingGroupName=msg['AutoScalingGroupName'],
                ForceDelete=True|False
    )
    # Delete load generator
    terminate_all_instances(target_ami=LOAD_GENERATOR_AMI)
    # Delete Load Balancer
    lb_client = boto3.client('elbv2', region_name='us-east-1')
    del_lb_res = lb_client.delete_load_balancer(
        LoadBalancerArn=msg['LoadBalancerArn']
    )  
    # Delete alarm
    cloudwatch = boto3.client('cloudwatch')
    cloudwatch.delete_alarms(
    AlarmNames=['Upper CPU Utilization'],
    )

def list_avail_subnets():
    ec2_client = boto3.client('ec2',region_name='us-east-1')
    sn_all = ec2_client.describe_subnets()['Subnets']
    return [sn['SubnetId'] for sn in sn_all]
    
def print_section(msg):
    """
    Print a section separator including given message
    :param msg: message
    :return: None
    """
    print(('#' * 40) + '\n# ' + msg + '\n' + ('#' * 40))

def is_test_complete(load_generator_dns, log_name):
    """
    Check if auto scaling test is complete
    :param load_generator_dns: lg dns
    :param log_name: log file name
    :return: True if Auto Scaling test is complete and False otherwise.
    """
    log_string = 'http://{}/log?name={}'.format(load_generator_dns, log_name)

    # creates a log file for submission and monitoring
    f = open(log_name + ".log", "w")
    log_text = requests.get(log_string).text
    f.write(log_text)
    f.close()
    return '[Test finished]' in log_text


def authenticate(load_generator_dns, submission_password, submission_username):
    """
    Authentication on LG
    :param load_generator_dns: LG DNS
    :param submission_password: SUBMISSION_PASSWORD
    :param submission_username: SUBMISSION_USERNAME
    :return: None
    """
    authenticate_string = 'http://{}/password?passwd={}&username={}'.format(
        load_generator_dns, submission_password, submission_username
    )
    response = None
    while not response or response.status_code != 200:
        try:
            response = requests.get(authenticate_string)
            break
        except requests.exceptions.ConnectionError:
            pass


########################################
# Main routine
########################################
def main():
    try:
        # BIG PICTURE TODO: Programmatically provision autoscaling resources
        #   - Create security groups for Load Generator and ASG, ELB
        #   - Provision a Load Generator
        #   - Generate a Launch Configuration
        #   - Create a Target Group
        #   - Provision a Load Balancer
        #   - Associate Target Group with Load Balancer
        #   - Create an Autoscaling Group
        #   - Initialize Warmup Test
        #   - Initialize Autoscaling Test
        #   - Terminate Resources
        subnet_id1, subnet_id2 = list_avail_subnets()[:2]
        terminate_all_instances(
            target_ami=[LOAD_GENERATOR_AMI, WEB_SERVICE_AMI])
        ec2_client = boto3.client('ec2', region_name='us-east-1')
        print_section('1 - create two security groups')

        PERMISSIONS = [
            {'IpProtocol': 'tcp',
             'FromPort': 80,
             'ToPort': 80,
             'IpRanges': [{'CidrIp': '0.0.0.0/0'}],
             'Ipv6Ranges': [{'CidrIpv6': '::/0'}],
             }
        ]
        # TODO: create two separate security groups and obtain the group ids
        # Security group for Load Generator instances
        response = ec2_client.describe_vpcs()
        vpc_id = response.get('Vpcs', [{}])[0].get('VpcId', '')
        sg1_id = create_security_groups(
            ec2_client, PERMISSIONS, vpc_id, "Load Generator", "In all-80-in Out all")
        # Security group for ASG, ELB instances
        sg2_id = create_security_groups(
            ec2_client, PERMISSIONS, vpc_id, "ASG and ELB", "In all-80-in Out all")
        print_section('2 - create LG')

        # TODO: Create Load Generator instance and obtain ID and DNS
        
        lg = create_instance(LOAD_GENERATOR_AMI, sg1_id, ins_type='t3.micro')
        lg_id = lg.instance_id
        lg_dns = lg.public_dns_name
        print("Load Generator running: id={} dns={}".format(lg_id, lg_dns))
        
        print_section('3. Create LC (Launch Config)')
        # TODO: create launch configuration
        asg_client = boto3.client('autoscaling', region_name='us-east-1')
        try:
            res = asg_client.create_launch_configuration(
                LaunchConfigurationName="EC2 Autoscaling Group",
                ImageId="ami-042de649749923897",
                KeyName="15619-CC",
                InstanceType='m5.large',
                InstanceMonitoring={
                        'Enabled': True
                },
                SecurityGroupIds=[
                    sg1_id,
                ],
                Tags=TAGS,
            )
        except Exception as e:  # launch configuration exists
            print(e)
            
        print_section('4. Create TG (Target Group)')
        # TODO: create Target Group
        try:
            elb_client = boto3.client('elbv2')
            tg_res = elb_client.create_target_group(
                Name="p1-ELB",
                Protocol='HTTP',
                Port=80,
                VpcId=vpc_id,
                HealthCheckProtocol='HTTP',
                HealthCheckEnabled=True,
                HealthCheckPath='/',
                TargetType='instance',
                Tags=TAGS
            )
        except Exception as e: 
            print(e)
            
        tg_arn = tg_res['TargetGroups'][0]['TargetGroupArn']
        print_section('5. Create ELB (Elastic/Application Load Balancer)')
        # TODO create Load Balancer
        # https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/elbv2.html
        lb = elb_client.create_load_balancer(
            Name='p1-ELB',
            SecurityGroups=[
                sg2_id,
            ], 
            Subnets=[subnet_id1,subnet_id2],
            Scheme='internet-facing', 
            Type='application',
            Tags=TAGS,
        )
        lb_arn = lb['LoadBalancers'][0]['LoadBalancerArn']
        lb_dns = lb['LoadBalancers'][0]['DNSName']
        print("lb started. ARN={}, DNS={}".format(lb_arn, lb_dns))

        print_section('6. Associate ELB with target group')
        # TODO Associate ELB with target group
        response = elb_client.create_listener(
            DefaultActions=[
                {
                    'TargetGroupArn': tg_arn,
                    'Type': 'forward',
                },
            ],
            Tags=TAGS,
            LoadBalancerArn=lb_arn,
            Port=80,
            Protocol='HTTP',
        )
        
        print_section('7. Create ASG (Auto Scaling Group)')
        # TODO create Autoscaling group
        #response =  asg_client.describe_auto_scaling_groups()
        response = asg_client.create_auto_scaling_group(
            AutoScalingGroupName='p1-ASG',
            TargetGroupARNs=[tg_arn],
            #HealthCheckGracePeriod=300,
            #HealthCheckType='ELB'
            MinSize=1,
            MaxSize=3,
            DesiredCapacity=2,
            DefaultCooldown=5*60,
            VPCZoneIdentifier=vpc_id,
            Tags=TAGS,
        )
        
        print_section('8. Create policy and attached to ASG')
        # TODO Create Simple Scaling Policies for ASG
        put_scaling_res = asg_client.put_scaling_policy(
            AutoScalingGroupName='p1-ASG',
            PolicyName='p1 EC2 Simple Scaling',
            PolicyType='SimpleScaling',
            AdjustmentType='PercentChangeInCapacity',
            MinAdjustmentStep=1,
            MinAdjustmentMagnitude=100,
            ScalingAdjustment=1,
        )
        attach_lb_asg_res = asg_client.attach_load_balancers(
            AutoScalingGroupName='p1-ASG',
            LoadBalancerNames=[
                'p1-ELB',
            ]
        )
        
        print_section('9. Create Cloud Watch alarm. Action is to invoke policy.')
        # TODO create CloudWatch Alarms and link Alarms to scaling policies
        cloudwatch = boto3.resource('cloudwatch')
        alarm = cloudwatch.Alarm('p1-cloudwatch')
        cloudwatch.put_metric_alarm(
            AlarmName='Upper CPU Utilization',
            ComparisonOperator='GreaterThanThreshold',
            EvaluationPeriods=1,
            MetricName='CPUUtilization',
            Namespace='AWS/EC2',
            Period=60,
            Statistic='Average',
            Threshold=80,
            ActionsEnabled=False,
            AlarmDescription='Alarm when server CPU exceeds 70%',
            Dimension={
                'Name':'AutoScalingGroupName',
                'Value':'p1-ASG'
            },
            Tags=TAGS,
            Unit='Seconds'
        )
        cloudwatch.put_metric_alarm(
            AlarmName='Lower CPU Utilization',
            ComparisonOperator='LowerThanThreshold',
            EvaluationPeriods=1,
            MetricName='CPUUtilization',
            Namespace='AWS/EC2',
            Period=60,
            Statistic='Average',
            Threshold=20,
            ActionsEnabled=False,
            AlarmDescription='Alarm when server CPU below 20%',
            Tags=TAGS,
            Dimension={
                'Name':'AutoScalingGroupName',
                'Value':'p1-ASG'
            },
            Unit='Seconds'
        )
        ##
        print_section('10. Authenticate with the load generator')
        authenticate(lg_dns, SUBMISSION_PASSWORD, SUBMISSION_USERNAME)

        print_section('11. Submit ELB DNS to LG, starting warm up test.')
        warmup_log_name = initialize_warmup(lg_dns, lb_dns)
        while not is_test_complete(lg_dns, warmup_log_name):
            time.sleep(1)

        print_section('12. Submit ELB DNS to LG, starting auto scaling test.')
        # May take a few minutes to start actual test after warm up test finishes
        log_name = initialize_test(lg_dns, lb_dns)
        while not is_test_complete(lg_dns, log_name):
            time.sleep(1)
        destroy_resources({
            'AutoScalingGroupName':'p1-ASG', 
            'LoadBalancerArn': lb_arn})
    except Exception as e:
        print(e)
        destroy_resources({
            'AutoScalingGroupName':'p1-ASG', 
            'LoadBalancerArn': lb_arn})


if __name__ == "__main__":
    main()
