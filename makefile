build:
	mvn install -U 
	mvn package -Dmaven.test.skip

build-testgo:
	cd go && make

spring-run: build
	mv ./target/inigo-jv-1.0.1.jar ./examples/spring/libs/ && cd ./examples/spring && ./gradlew clean bootRun --refresh-dependencies
