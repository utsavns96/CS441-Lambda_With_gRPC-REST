import json
import boto3
import os
import datetime
import re
import hashlib

def lambda_handler(event, context):
    head = list()
    tail = list()
    resultlist = list()
    pattern=re.compile(os.environ['pattern'])
    s3 = boto3.client('s3')
    #Checking if the query has input as query string parameters or json
    if event['queryStringParameters']:
        t = event['queryStringParameters']['T']
        dt = event['queryStringParameters']['dT']
    else:
        t = json.loads(event['body'])['T']
        dt = json.loads(event['body'])['dT']
    #function for binary search
    def binarysearch(low,mid,high,timetocheck,logfile):
        print('In binary search')
        while low<=high:
            mid = (high+low)//2
            timenow = int(logfile[mid][0:2])*60*60 + int(logfile[mid][3:5])*60 + int(logfile[mid][6:8])
            if (timenow>timetocheck):
                high=mid-1
            elif (timenow<timetocheck):
                low=mid+1
            else:
                print('Breaking out of while')
                break
        return mid
    #function to get the MD5 hash for log strings
    def md5resp():
        print('in md5resp')
        logfile = list()
        logfile=s3.get_object(Bucket=os.environ['s3bucket'],Key=os.environ['filepath']).get('Body').read().decode('utf-8')
        logfile = "".join(logfile).replace("\r", "").split("\n")
        logfile.pop()
        #converting time to seconds to perform a binary search
        tsecs = int(t[0:2])*60*60 + int(t[3:5])*60 + int(t[6:8])
        dtsecs = int(dt[0:2])*60*60 + int(dt[3:5])*60 + int(dt[6:8])
        lowsecs = tsecs - dtsecs
        highsecs = tsecs + dtsecs

        logindext = binarysearch(0,0,len(logfile)-1,tsecs,logfile)
        logindexlow = binarysearch(0,0,logindext,lowsecs,logfile)
        logindexhigh = binarysearch(logindexlow,0,len(logfile)-1,highsecs,logfile)
        print('getting our messages')
        for item in range(logindexlow,logindexhigh+1):
            if pattern.search(logfile[item]):
                print('Found a log message')
                resultlist.append(hashlib.md5(logfile[item].encode('utf-8')).hexdigest())
        print(resultlist)
        return resultlist
 
    #get the first few bytes of the file
    head = s3.get_object(Bucket=os.environ['s3bucket'],Key=os.environ['filepath'],Range=os.environ['headrange']).get('Body').read().decode('utf-8')
    head = "".join(head).split("\r")[0].replace("\n", "")
    #getting the last few bytes of the file
    tail = s3.get_object(Bucket=os.environ['s3bucket'],Key=os.environ['filepath'],Range=os.environ['tailrange']).get('Body').read().decode('utf-8')
    tail = "".join(tail).split("\r")[-2].replace("\n", "")
    #get the first and last timestamp of the log file
    firstlog = datetime.time(int(head[0:2]),int(head[3:5]))
    lastlog = datetime.time(int(tail[0:2]),int(tail[3:5]))
    
    #convert the input to time
    inputtime=datetime.time(int(t[0:2]), int(t[3:5]))
    timechange = datetime.timedelta(hours=int(dt[0:2]),minutes=int(dt[3:5]))
    #using datetime to allow us to add and subtract time, and then compare it
    low = (datetime.datetime.combine(datetime.date(1,1,1),inputtime) - timechange).time()
    high = (datetime.datetime.combine(datetime.date(1,1,1),inputtime) + timechange).time()
    print('firstlog=',firstlog)
    print('lastlog=',lastlog)
    print('low=',low)
    print('high=',high)
    #if the time window in input lies in the start and end of the log file
    if(firstlog <= low and lastlog >= high):
            print('trying to get md5 hash')
            result=md5resp()
            print('result=', result)
            return{
                'statusCode': 200,
                'body': ",".join(result)
            }
    else:
        print('The time window does not exist in this log file')
        return {
        'statusCode': 404,
        'body': json.dumps('The time window does not exist in this log file')
        }
