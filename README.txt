This repository contains all supplementary materials, appendices, code, and documentation accompanying the
paper titled 

Is  Impulsive Behavior Adaptive in Harsh and Unpredictable Environments? A Formal Model 
By [AUTHOR(S) BLINDED]

Before using the graphical user interface, please read the following notes below.

--- This repository contains an ECLIPSE project ---
I developed this program in 2018 and 2019 using ECLIPSE Oxygen.2 Release (4.7.2) on a WINDOWS 10
device. The GUI uses JavaFX, created with Gluon Scene Builder (9.0.1). Although this project *should*
work with other IDE's and operating systems, I have only tested it on ECLIPSE and WINDOWS 10.

--- INSTALLATION NOTES --- 
Step 0. Please clone all files to your device. 

Step 1. The main code of the program is written in JAVA (version 8.192). As such, please install
a JDK (Java development kit) with a version of 8.192 or higher on your device.

Step 2. Although most of the code runs in JAVA, the program uses R for some calculations in the
background. Please install R with a version of 3.4.2 or higher on your device.

Step 3. This communication between JAVA and R is handled by an external package, called
"Rserve". Before use, please install this package via the R command line ("install.packages('Rserve')")

Step 4. Open the JAVA project in ECLIPSE or your IDE of choice. Navigate to Model>start>Model and execute
the JAVA script from here.

Step 5 (if things go wrong). When starting the ADAPTIVE INFORMATION IMPULSIVITY the first time, it prompts
JAVA to look for your R directory. This might take a while. If successful, the program will then try to start
an Rserve instance automatically. However, for reasons unknown, this does not always work in some devices. If
this is the case, the program will remain a white screen (and is possibly marked as unresponsive by your OS). 
If this is the case for you, please start R and run the following code: "library(Rserve); Rserve()" (excluding 
the "s). After having done so the JAVA program should detect the Rserve client, and boot successfully. 

Step 6. Please read the information on the introduction page. In addition, please read through appendices A to D.


--- In case of errors, issues, comments, or feedback ---
This is my first time using a GITHUB repository. If there are any problems, suggestions, etc.,  please 
contact the first author at [AUTHOR(S) BLINDED]


--- A note on the inconsistency of terms ---
There are several differences between the terms used in the paper, the visual interface, and the JAVA code.
The reason for these differences is that we decided on different terms throughout this project. To preserve 
backwards compatibility with older versions, it was not always possible to update these terms. A couple of 
common inconsistencies are:

- The resource quality is often referred to as the resource value
- Accepting is often referred to as eating
- Rejecting is often referred to as discarding

We apologize for this inconsistency. If anything is unclear, please contact the first author [AUTHOR(S) BLINDED].
