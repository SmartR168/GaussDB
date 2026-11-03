#!/usr/bin/env bash
set -euo pipefail
cd /root/finance-lab
mkdir -p libs logs build data config
[ "$(uname -m)" = x86_64 ] || { echo 'Expected x86_64 client host'; exit 1; }
FINANCE_DRIVER=/root/GaussDB_driver/Centralized/Euler2.5_X86_64
if [ ! -d "$FINANCE_DRIVER" ]; then
 wget -q https://dbs-download.obs.cn-north-1.myhuaweicloud.com/GaussDB/1716897684140/GaussDB_driver.zip -O /root/GaussDB_driver.zip
 unzip -oq /root/GaussDB_driver.zip -d /root
fi
tar -xzf "$FINANCE_DRIVER/GaussDB-Kernel_505.1.0_Euler_64bit_Gsql.tar.gz" -C /opt
tar -xzf "$FINANCE_DRIVER/GaussDB-Kernel_505.1.0_Euler_64bit_Jdbc.tar.gz" -C libs
echo '72ffc8da0de0a41c7c6b6c71f727c78a344a97d06e3d7449bcfeb9c90f580c71  libs/gaussdbjdbc.jar' | sha256sum -c -
cd /root
if [ ! -d /root/jdk-11.0.10+9 ]; then
 wget -q https://edu-res.hc-cdn.cn/sharedata/hdinnovation/sandbox/lab2024/20220525/OpenJDK11U-jdk_x64_linux_openj9_linuxXL_11.0.10_9_openj9-0.24.0.tar.gz -O jdk11.tar.gz
 tar -xzf jdk11.tar.gz
fi
cat > /root/finance-lab/scripts/env.sh <<'ENV'
export JAVA_HOME=/root/jdk-11.0.10+9
export PATH="$JAVA_HOME/bin:/opt/bin:$PATH"
export LD_LIBRARY_PATH="/opt/lib:${LD_LIBRARY_PATH:-}"
export GS_KTOOL_FILE_PATH=/opt/gs_ktool_file
ENV
cd /root/finance-lab
set +u
source scripts/env.sh
set -u
{ date -Is; hostname; uname -m; cat /etc/centos-release; java -version; javac -version; gsql --version; sha256sum libs/gaussdbjdbc.jar; } 2>&1 | tee logs/client_environment.txt
bash scripts/compile.sh
echo FINANCE_CLIENT_READY
