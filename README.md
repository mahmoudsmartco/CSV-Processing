CSV Processing REST API described as the below functionality:

1-	Request to enter the CSV files paths over secure channel like SFTP. 

2-	Transfer files in a secure way

    •	Transfer the CSV files over a secure protocol like SFTP to local path on Central Processing Engine.
    
    •	Handle files from different locations by configuring multiple file paths.
    
3-	Read and Parse CSV Files

    •	Use a Java library like Apache Commons CSV to efficiently read and parse the CSV files.
    
    •	Parse each line of the CSV files to extract National ID, Name, and Amount fields.
    
4-	Processing Logic filter data based on the amount:

    •	For amounts below 1000 EGP, calculate fees (10%) and store in a new generated file and stream it to the first topic.
    
    •	For amounts above 1000 EGP, convert to USD using the exchange rate (20 EGP per dollar), calculate fees (20%), store in a different file and stream it to the second topic.
    
    •	For incorrectly parsed data, store in exception file and stream it to the third topic.
    
5-	Install / configure Kafka Engine

    •	Set up a Kafka instance with three topics for processing the data.
    
    •	Configure a Kafka producer within the microservice to stream data to the Kafka topics.
    
6-	Storing the Result:

    •	Store the processed data into separate file based on the processing logic.
    
    •	Prepare log file for error handling and logging to track any issues during processing.

Tools: 

1- KAFKA 

2- Xlight FTP Server for creating SFTP on windows  link: "https://www.xlightftpd.com/download/setup-x64.exe"

3- WinSCP to test SFTP server and upload download CSV files.

4- POSTMAN to call REST API with the below:

    URL : 
    "http://localhost:8080/api/csv/process"
    
    RequestBody :
    "[
        {
            "serverIp":"192.168.1.8" ,
            "serverPort":"22",
            "serverUser":"mm22",
            "serverPass":"mm22",
            "directoryPath":"/location1/"
        },
        {
            "serverIp":"192.168.1.8" ,
            "serverPort":"22",
            "serverUser":"mm22",
            "serverPass":"mm22",
            "directoryPath":"/location2/"
        },
        {
            "serverIp":"192.168.1.8" ,
            "serverPort":"22",
            "serverUser":"mm22",
            "serverPass":"mm22",
            "directoryPath":"/location3/"
        },
        {
            "serverIp":"192.168.1.8" ,
            "serverPort":"22",
            "serverUser":"mm22",
            "serverPass":"mm22",
            "directoryPath":"/location4/"
        }

    
]
"
    

![image](https://github.com/mahmoudsmartco/CSV-Processing/assets/138441771/280d5dbe-82b6-4415-9157-b3c0e8050e8e)



Demo 


https://youtu.be/cOF2gPb1imo




