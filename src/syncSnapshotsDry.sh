#Sync with all Remote snapshots (dry run)
# sudo apt-get install openjdk-11-jre -y
# sudo chmod go+rw /opt
# file lacation: /opt/syncSnapshotsDry.sh
# sudo chmod +x /opt/syncSnapshotsDry.sh
# sudo apt-get install curl -y
# sh /opt/syncSnapshotsDry.sh

sudo java /opt/bssm.java -RD=/dev/nvme0n1 -syncSnapshots=dry
