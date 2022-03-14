#!/bin/bash
CLASSPATH="src/"
TOTAL_STEPS=0
CONNECTION_LIMIT=0
DEBUG=""

while getopts "c:s:d" OPTION
do 
   case $OPTION in
      c)
         CONNECTION_LIMIT=$OPTARG
         ;;
      s)
         TOTAL_STEPS=$OPTARG
         ;;
      d)
         DEBUG="-d"
         ;;
       ?)
         echo "ERROR: Unknown argument(s)"
         echo "Please set valid numbers to Connection limit argument -c [Connectio_limit] and  Total steps -s [Step_limit]"
         echo "Example valid execution is command: sh start_server.sh -c 2 -s 100"
         exit -1
         ;;
     esac
done

echo "Starting server..."

# Messy way to move .class files, but it works.
javac -cp . src/overseer/*.java
rm -r out/
mkdir out
mkdir out/overseer
mv src/overseer/*.class out/overseer/
cd out
java -cp . overseer/Main -s $TOTAL_STEPS -c $CONNECTION_LIMIT $DEBUG

