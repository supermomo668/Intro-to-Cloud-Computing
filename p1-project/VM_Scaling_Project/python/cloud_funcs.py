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

def get_vpc_id(client):
    ec2_client = boto3.client('ec2', region_name='us-east-1')
    return ec2_client.describe_vpcs().get('Vpcs', [{}])[0].get('VpcId', '')
    
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
        if 'rps' in sec:
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