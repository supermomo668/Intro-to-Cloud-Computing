import re
import json
import time
import requests
import botocore
import boto3
import sys
import os
sys.path.append(os.path.abspath('../'))
from cloud_funcs import *

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


def destroy_resources(InstancesAMI=[LOAD_GENERATOR_AMI, WEB_SERVICE_AMI],
                      msg: dict = {
        'ASGConfigurationName': [configuration['launch_configuration_name']],
        'AutoScalingGroupName': [configuration['auto_scaling_group_name']],
        'LoadBalancerName': [configuration['load_balancer_name']],
        'TargetGroupName': [configuration['auto_scaling_target_group']], }
):
    """
    Delete all resources created for this task
    """
    # TODO: implement this method
    # Delete load generator
    print("Deleting Instances ...")
    terminate_all_instances(target_ami=InstancesAMI)
    # Delete launch config and instances and ASG
    asg_client = boto3.client('autoscaling', region_name='us-east-1')
    try:    # Delete ASG resources
        print("Deleting ASG ...", msg['AutoScalingGroupName'])
        [asg_client.update_auto_scaling_group(
            AutoScalingGroupName=name,
            MinSize=0, MaxSize=0,
            DesiredCapacity=0) for name in msg['AutoScalingGroupName']]
        [asg_client.delete_auto_scaling_group(
            AutoScalingGroupName=name, ForceDelete=True) for name in msg['AutoScalingGroupName']]
    except Exception as e:
        print(e)
    try:    # Delete launch config
        print("Deleting launch config...", msg['ASGConfigurationName'])
        [asg_client.delete_launch_configuration(
            LaunchConfigurationName=name) for name in msg['ASGConfigurationName']]
    except Exception as e:
        print(e)
    # Delete Load Balancer
    elb_client = boto3.client('elbv2', region_name='us-east-1')
    cloudwatch = boto3.client('cloudwatch')
    try:
        all_elbs = elb_client.describe_load_balancers()
        print("To delete load balancer", msg['LoadBalancerName'])
        for elb in all_elbs['LoadBalancers']:
            print("Found", elb['LoadBalancerName'])
            if elb['LoadBalancerName'] in msg['LoadBalancerName']:
                elb_client.modify_load_balancer_attributes(Attributes=[{
                    'Key': 'deletion_protection.enabled',
                    'Value': 'false'}],
                    LoadBalancerArn=elb['LoadBalancerArn'])
                elb_client.delete_load_balancer(
                    LoadBalancerArn=elb['LoadBalancerArn'])
        # Delete alarm
        cloudwatch.delete_alarms(
            AlarmNames=['Upper CPU Utilization', 'Lower CPU Utilization'],)
    except Exception as e:
        print(e)
    try:
        all_tg = elb_client.describe_target_groups()
        for elb in all_tg['TargetGroups']:
            if elb['TargetGroupName'] in msg['TargetGroupName']:
                # Delete target group
                elb_client.delete_target_group(
                    TargetGroupArn=elb['TargetGroupArn'])
    except Exception as e:
        print(e)


def list_avail_subnets():
    ec2_client = boto3.client('ec2', region_name='us-east-1')
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
        ec2_client = boto3.client('ec2', region_name='us-east-1')
        vpc_id = get_vpc_id(ec2_client)
        all_subnets = list_avail_subnets()
        print("Found", len(all_subnets), "available subnets")
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
        sg1_id = create_security_groups(
            ec2_client, PERMISSIONS, vpc_id, "Load Generator", "In all-80-in Out all")
        # Security group for ASG, ELB instances
        sg2_id = create_security_groups(
            ec2_client, PERMISSIONS, vpc_id, "ASG and ELB", "In all-80-in Out all")
        print_section('2 - create LG')

        # TODO: Create Load Generator instance and obtain ID and DNS
        lg = create_instance(LOAD_GENERATOR_AMI, sg1_id,
                             ins_type=configuration['instance_type'])
        lg_id = lg.instance_id
        lg_dns = lg.public_dns_name
        print("Load Generator running: id={} dns={}".format(lg_id, lg_dns))

        print_section('3. Create LC (Launch Config)')
        # TODO: create launch configuration
        asg_client = boto3.client('autoscaling', region_name='us-east-1')

        def launch_config():
            return asg_client.create_launch_configuration(
                LaunchConfigurationName=configuration['launch_configuration_name'],
                ImageId=WEB_SERVICE_AMI,
                KeyName="15619-CC",
                InstanceType=configuration['instance_type'],
                InstanceMonitoring={
                        'Enabled': True
                },
                SecurityGroups=[
                    sg2_id,
                ],
            )
        try:
            res = launch_config()
        except Exception as e:  # launch configuration exists
            print(e)
            asg_client.delete_launch_configuration(
                LaunchConfigurationName=configuration['launch_configuration_name'])
            res = launch_config()
        print_section('4. Create TG (Target Group)')
        # TODO: create Target Group
        try:
            elb_client = boto3.client('elbv2')
            tg_res = elb_client.create_target_group(
                Name=configuration['auto_scaling_target_group'],
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
            Name=configuration['load_balancer_name'],
            SecurityGroups=[
                sg2_id,
            ],
            Subnets=all_subnets,
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
        try:
            response = asg_client.create_auto_scaling_group(
                AutoScalingGroupName=configuration['auto_scaling_group_name'],
                LaunchConfigurationName=configuration['launch_configuration_name'],
                TargetGroupARNs=[tg_arn],
                HealthCheckType='EC2',
                MinSize=configuration['asg_min_size'],
                MaxSize=configuration['asg_max_size'],
                DesiredCapacity=configuration['asg_desired_capacity'],
                DefaultCooldown=configuration['asg_default_cooldown_period'],
                HealthCheckGracePeriod=configuration['health_check_grace_period'],
                VPCZoneIdentifier=','.join(all_subnets),
                Tags=[
                    {
                        'ResourceId': configuration['auto_scaling_group_name'],
                        'ResourceType': 'auto-scaling-group',
                        'Key': 'Project',
                        'Value': 'vm-scaling',
                        'PropagateAtLaunch': True
                    }],
            )
        except Exception as e:  # already existed
            print(e)

        print_section('8. Create policy and attached to ASG')
        # TODO Create Simple Scaling Policies for ASG
        put_scalingout_res = asg_client.put_scaling_policy(
            AutoScalingGroupName=configuration['auto_scaling_group_name'],
            PolicyName='p1 EC2 Simple Scaling-out',
            PolicyType='SimpleScaling',
            AdjustmentType='ChangeInCapacity',
            ScalingAdjustment=configuration['scale_out_adjustment'],
            Cooldown=configuration['cooldown_period_scale_out'],
        )
        put_scalingin_res = asg_client.put_scaling_policy(
            AutoScalingGroupName=configuration['auto_scaling_group_name'],
            PolicyName='p1 EC2 Simple Scaling-in',
            PolicyType='SimpleScaling',
            AdjustmentType='ChangeInCapacity',
            ScalingAdjustment=configuration['scale_in_adjustment'],
            Cooldown=configuration['cooldown_period_scale_in']
        )

        attach_lb_asg_res = asg_client.attach_load_balancer_target_groups(
            AutoScalingGroupName=configuration['auto_scaling_group_name'],
            TargetGroupARNs=[tg_arn],
        )
        print("Attach LB to ASG:", attach_lb_asg_res['ResponseMetadata'])
        print_section(
            '9. Create Cloud Watch alarm. Action is to invoke policy.')
        # TODO create CloudWatch Alarms and link Alarms to scaling policies
        cw_client = boto3.client('cloudwatch')
        cw_client.put_metric_alarm(
            AlarmName='Upper CPU Utilization',
            ComparisonOperator='GreaterThanOrEqualToThreshold',
            MetricName='CPUUtilization',
            Namespace='AWS/EC2',
            ActionsEnabled=True,
            Period=configuration['alarm_period'],
            EvaluationPeriods=configuration['alarm_evaluation_periods_scale_out'],
            Statistic='Maximum',
            Threshold=configuration['cpu_upper_threshold'],
            Dimensions=[
                {"Name": "AutoScalingGroupName",
                 "Value": configuration['auto_scaling_group_name']}
            ],
            AlarmDescription='Alarm when server CPU exceeds 70%',
            AlarmActions=[put_scalingout_res['PolicyARN']],
            Tags=TAGS,
            Unit='Seconds'
        )
        cw_client.put_metric_alarm(
            AlarmName='Lower CPU Utilization',
            ComparisonOperator='LessThanThreshold',
            MetricName='CPUUtilization',
            Namespace='AWS/EC2',
            ActionsEnabled=True,
            Period=configuration['alarm_period'],
            EvaluationPeriods=configuration['alarm_evaluation_periods_scale_in'],
            Statistic='Maximum',
            Threshold=configuration['cpu_lower_threshold'],
            AlarmDescription='Alarm when server CPU below 30%',
            Tags=TAGS,
            Dimensions=[
                {"Name": "AutoScalingGroupName",
                 "Value": configuration['auto_scaling_group_name']}
            ],
            AlarmActions=[put_scalingin_res['PolicyARN']],
            Unit='Seconds'
        )
        # Enabled ASG to be monitored
        ##
        print_section('10. Authenticate with the load generator')
        authenticate(lg_dns, SUBMISSION_PASSWORD, SUBMISSION_USERNAME)

        print_section('11. Submit ELB DNS to LG, starting warm up test.')
        #warmup_log_name = initialize_warmup(lg_dns, lb_dns)
        # while not is_test_complete(lg_dns, warmup_log_name):
        #    time.sleep(1)

        print_section('12. Submit ELB DNS to LG, starting auto scaling test.')
        # May take a few minutes to start actual test after warm up test finishes
        log_name = initialize_test(lg_dns, lb_dns)
        print("Using log name:", log_name)
        while not is_test_complete(lg_dns, log_name):
            print("RPS:", get_rps(lg_dns, log_name))
            time.sleep(1)
        destroy_resources()
    except Exception as e:
        print(e)
        destroy_resources()


if __name__ == "__main__":
    destroy_resources()
    main()
