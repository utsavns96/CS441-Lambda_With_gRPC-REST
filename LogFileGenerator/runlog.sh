now="$(date '+%Y-%m-%d')"
sbt clean compile run;
filenameprepend="LogFileGenerator."
filename="$filenameprepend$now.log"
cd log;
unix2dos "$filename"
aws s3 cp "$filename" s3://f22-cs441hw2/input/input.log