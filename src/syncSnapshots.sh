#Sync with all Remote snapshots
# sudo apt-get install openjdk-11-jre -y
# sudo chmod go+rw /opt
# file lacation: /opt/syncSnapshots.sh
# sudo chmod +x /opt/syncSnapshots.sh
# sudo apt-get install curl -y
# sh /opt/syncSnapshots.sh

sudo java /opt/bssm.java -RD=/dev/nvme0n1 -syncSnapshots=full
