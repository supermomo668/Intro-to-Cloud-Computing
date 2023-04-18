	#1. SSH to the VM and update the package index
sudo apt-get update
	#2. Install Java 8
#sudo apt install openjdk-8-jre-headless
	#3. Install Scala 2.11
wget http://www.scala-lang.org/files/archive/scala-2.11.7.tgz
tar xvf scala-2.11.7.tgz
sudo mv scala-2.11.7 /usr/lib
sudo ln -s /usr/lib/scala-2.11.7 /usr/lib/scala
echo "export PATH=$PATH:/usr/lib/scala/bin" >> ~/.bashrc
source ~/.bashrc
rm scala-2.11.7.tgz
	#4. Install Python 3
#sudo apt install python3
	#5. Install Apache Spark. You can find a list of Spark releases here and download archived versions here.
wget https://archive.apache.org/dist/spark/spark-2.4.5/spark-2.4.5-bin-hadoop2.7.tgz
tar -xvf spark-2.4.5-bin-hadoop2.7.tgz
echo "export PATH=$PATH:/home/ubuntu/spark-2.4.5-bin-hadoop2.7/bin" >> ~/.bashrc
source ~/.bashrc
    #6.Run a sample program
run-example SparkPi
