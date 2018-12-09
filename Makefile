clean:
	echo "¬ cleaning class files"
	find . -name "*.class" -delete

build:
	echo "¬ building class files"
	mkdir -p .artifacts/build
	javac -d .artifacts/build -sourcepath ./src src/com/spotripy/App.java

run:
	echo "¬ running project"
	java -cp .artifacts/build com.spotripy.App

clean-build: clean build

run-build: clean-build run

ifndef VERBOSE
.SILENT:
endif
