import java.util.*
import java.util.*
projectUrl = 'https://github.com/snp-technologies/sample-cicd-java.git'
GitCredentials = 'GitCredentials'
def checkout(def branch,projectUrl,def Credentials) 
{
	checkout([
		$class: 'GitSCM', 
		branches: [[name:branch]],                         
		userRemoteConfigs: [[
		credentialsId:Credentials, 
		url: projectUrl
		]],
		extensions: [[
		$class: 'LocalBranch', 
		localBranch: "**"], 
		[$class: 'SubmoduleOption',
		parentCredentials: true, 
		recursiveSubmodules: true, 
		trackingSubmodules: true
		]]
	])
}
def emailnotification(String version, String buildstatus) {
	emailext (
	subject: "Jenkins job Name:: ${env.JOB_NAME} - Version: ${version} - Build Status: "+currentBuild.currentResult,
body: 
"""
PROJECT :${env.JOB_NAME}
BUILD NUMBER :${env.BUILD_NUMBER}
Version :${version}
Build Status :${currentBuild.currentResult}
Check console output at : ${env.BUILD_URL}console
""",
	to: "${env.Email_Recipients}",
	attachLog: true
	 )
 }

def failureemailnotification(String version, String buildstatus,String errormsg) {
	emailext (
	subject: "Jenkins job Name:: ${env.JOB_NAME} - Version: ${version} - Build Status: "+currentBuild.currentResult,
body: 
"""
PROJECT :${env.JOB_NAME}
BUILD NUMBER :${env.BUILD_NUMBER}
Version :${version}
Error :${errormsg}
Check console output at : ${env.BUILD_URL}console
""",
	to: "${env.Email_Recipients}",
	attachLog: true
	 )
 } 
node
{
	try
	{
		mvnHome = tool 'mvnHome'
		javaHome = tool 'javaHome'
		def workspace = pwd()
	
		stage('Code Checkout')
		{
			dir("${workspace}")
			{
				branch = "${env.Git_Branch}"
				checkout("${branch}","${projectUrl}","${GitCredentials}")
			}
		}
		stage('Versioning')
		{
			versionnumber = "1.0."+"${env.BUILD_NUMBER}"
		}
		stage('Clean the Workspace')
		{
       
         bat(/"${mvnHome}\bin\mvn" clean/)
		}
		withEnv(["PATH+JDK=$javaHome/bin"])
		{
			stage('Compile')
			{
			   
				 bat(/"${mvnHome}\bin\mvn" compile/)
			}
			stage('Build')
			{
			   
				 bat(/"${mvnHome}\bin\mvn" package/)
			}
		}	
	}
	catch(Exception e){
			currentBuild.result = 'FAILURE'
			echo "Current Build Status is ${currentBuild.result}"
			failureemailnotification("${versionnumber}","${currentBuild.result}","${e.message}")
			error e.message
	}
	finally
	{
		stage('notify'){
				if(("${currentBuild.result}"!="FAILURE")&&("${currentBuild.result}"!="NOT_BUILT"))
				{
					emailnotification("${versionnumber}", "${currentBuild.result}")
				}	
				
		}	
	}
}	
	
