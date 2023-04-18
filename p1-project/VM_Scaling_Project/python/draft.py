    
    
def __name__=="__main__":
    #Initialize parameters for load scaling
    cpu_utilization = []
    all_web_instances = [web_ins.instance_id]
    launch_times = [get_test_start_time(lg_dns, logger)]
    time_interval = 5   # minutes
    while not is_test_complete(lg_dns, logger):
        n_instance = len(all_web_instances)
        # TODO: Check RPS and last launch time
        rps = get_rps(lg_dns, logger)
        # TODO: Add New Web Service Instance if Required
        # append average cpu util of all isntance
        avg_cpu = cloud_watch_avg_cpu_all(all_web_instances)
        cpu_utilization.append(avg_cpu)
        launch_times.append(datetime.utcnow())
        # search where is the last 5 minute launch_time
        for idx in launch_times:
            if (datetime.utcnow()-launch_times[idx]).total_seconds() <= time_interval*60:
                break
        cpu_utilization = cpu_utilization[idx:]
        interval_cpu_utiiization = sum(cpu_utilization)/len(cpu_utilization)
        # Scale out
        if interval_cpu_utiiization >= 0.8 and \
            (datetime.utcnow()-launch_times[-1]).total_seconds()>=100:
            web_ins = add_web_service_instance(
                lg_dns=lg_dns, sg2_name=sg2_id, log_name=logger)
            all_web_instances.append(web_ins.instance_id)
        elif interval_cpu_utiiization <= 0.2 and len(all_web_instances)>0:  # Scale in
            response = ec2_client.stop_instances(
                InstanceIds=[
                    all_web_instances[-1],
                ],
            )
            all_web_instances.pop()
        time.sleep(1)