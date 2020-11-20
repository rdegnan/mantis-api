#!/bin/bash

# create the mantisapi binary
./gradlew clean installDist

cp ../mantis/mantis-examples/mantis-examples-twitter-sample/build/distributions/mantis-examples-twitter-sample-1.4.0-SNAPSHOT.zip .
cp ../mantis/mantis-examples/mantis-examples-twitter-sample/build/distributions/mantis-examples-twitter-sample-1.4.0-SNAPSHOT.json .
cp ../mantis/mantis-examples/mantis-examples-sine-function/build/distributions/mantis-examples-sine-function-1.4.0-SNAPSHOT.zip .
cp ../mantis/mantis-examples/mantis-examples-sine-function/build/distributions/mantis-examples-sine-function-1.4.0-SNAPSHOT.json .
cp ../mantis/mantis-examples/mantis-examples-groupby-sample/build/distributions/mantis-examples-groupby-sample-1.4.0-SNAPSHOT.zip .
cp ../mantis/mantis-examples/mantis-examples-groupby-sample/build/distributions/mantis-examples-groupby-sample-1.4.0-SNAPSHOT.json .
cp ../mantis/mantis-examples/mantis-examples-synthetic-sourcejob/build/distributions/mantis-examples-synthetic-sourcejob-1.4.0-SNAPSHOT.zip .
cp ../mantis/mantis-examples/mantis-examples-synthetic-sourcejob/build/distributions/mantis-examples-synthetic-sourcejob-1.4.0-SNAPSHOT.json .
cp ../mantis/mantis-examples/mantis-examples-jobconnector-sample/build/distributions/mantis-examples-jobconnector-sample-1.4.0-SNAPSHOT.zip .
cp ../mantis/mantis-examples/mantis-examples-jobconnector-sample/build/distributions/mantis-examples-jobconnector-sample-1.4.0-SNAPSHOT.json .

cp ../mantis/mantis-source-jobs/mantis-source-job-kafka/build/distributions/mantis-source-job-kafka-1.4.0-SNAPSHOT.zip .
cp ../mantis/mantis-source-jobs/mantis-source-job-kafka/build/distributions/mantis-source-job-kafka-1.4.0-SNAPSHOT.json .

cp ../mantis/mantis-source-jobs/mantis-source-job-publish/build/distributions/mantis-source-job-publish-1.4.0-SNAPSHOT.json .
cp ../mantis/mantis-source-jobs/mantis-source-job-publish/build/distributions/mantis-source-job-publish-1.4.0-SNAPSHOT.zip .

# build the docker image

docker build -t dev/mantisapi .

echo "Created Docker image 'dev/mantisapi'"
