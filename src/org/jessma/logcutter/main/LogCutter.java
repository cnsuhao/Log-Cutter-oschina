/*
 * Copyright Bruce Liang (ldcsaa@gmail.com)
 *
 * Version	: Log-Cutter 2.0.2
 * Author	: Bruce Liang
 * Website	: http://www.jessma.org
 * Project	: https://github.com/ldcsaa
 * Blog		: http://www.cnblogs.com/ldcsaa
 * WeiBo	: http://weibo.com/u/1402935851
 * QQ Group	: 75375912
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jessma.logcutter.main;

import static java.lang.System.out;
import static org.jessma.logcutter.global.AppConfig.currentProcessId;
import static org.jessma.logcutter.global.AppConfig.getAppName;
import static org.jessma.logcutter.global.AppConfig.getAppVersion;
import static org.jessma.logcutter.global.AppConfig.getArcFiles;
import static org.jessma.logcutter.global.AppConfig.getCheckInterval;
import static org.jessma.logcutter.global.AppConfig.getCutFiles;
import static org.jessma.logcutter.global.AppConfig.getDefaultConfigFile;
import static org.jessma.logcutter.global.AppConfig.getDelFiles;
import static org.jessma.logcutter.global.AppConfig.getLockFile;
import static org.jessma.logcutter.global.AppConfig.getLog4jConfigFile;
import static org.jessma.logcutter.global.AppConfig.getStartCheckDelay;
import static org.jessma.logcutter.global.AppConfig.hasArcFiles;
import static org.jessma.logcutter.global.AppConfig.hasCutFiles;
import static org.jessma.logcutter.global.AppConfig.hasDelFiles;
import static org.jessma.logcutter.global.AppConfig.summary;

import java.io.Console;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.jessma.logcutter.global.AppConfig;
import org.jessma.logcutter.runner.ArcFileRunner;
import org.jessma.logcutter.runner.CutFileRunner;
import org.jessma.logcutter.runner.DelFileRunner;
import org.jessma.logcutter.runner.FileRunner;
import org.jessma.logcutter.util.LogUtil;

import static org.jessma.logcutter.util.GeneralHelper.IS_WINDOWS_PLATFORM;
import static org.jessma.logcutter.util.GeneralHelper.NEWLINE_CHAR;
import static org.jessma.logcutter.util.GeneralHelper.getJavaVersion;
import static org.jessma.logcutter.util.GeneralHelper.printExceptionMessageStack;


public class LogCutter
{
	private static Logger logger					= null;
	
	private static final String REQ_JAVA_VERSION	= "1.6";
	private static final String SHOW_HELP_KEY		= "HELP";
	private static final String SHOW_JOBS_KEY		= "JOBS";
	private static final String SHOW_SUMMARY_KEY	= "CFG";
	private static final String RUN_AT_ONCE_KEY		= "RUN";
	private static final String SHUTDOWN_DOWN_KEY	= "!Q";
	private static final String SHOW_ABOUT_KEY		= "?";
	private static final int SHUTDOWN_AWAIT_TIME	= 3;
	private static final TimeUnit SCHEDULE_TIMEUNIT	= TimeUnit.MINUTES;
	private static final Console CONSOLE			= System.console();
	private static final String USAGE				= String.format("java %s [ -1 ] [ -f <config-file> ]", LogCutter.class.getName());

	private static FileLock lock;
	private static List<FileRunner> runners			= new ArrayList<FileRunner>();
	private static ScheduledThreadPoolExecutor sc 	= new ScheduledThreadPoolExecutor(0);
	private static boolean isRunOnce				= false;
	private static volatile boolean hasShutdown		= false;

	public static void main(String[] args)
	{
		try
		{
            checkJavaVersion();
            
            String configFile = parseArgs(args);
            AppConfig.init(configFile);
            
            checkSingleInstance();
            
            LogUtil.initialize(getLog4jConfigFile());
            LogUtil.setDefaultLoggerName(getAppName());
            logger = LogUtil.getDefaultLogger();
            
    		loadRunners();
            
    		if(isRunOnce)
            	runOnce();
    		else
            {
	            startLogCutSchedule();
	            waitForDirection();
            }
            
		}
		catch(Exception e)
		{
			printExceptionMessageStack(e, out);
		}
		finally
		{
			releaseLock();
		}
	}

	private static void checkJavaVersion()
	{
		String version = getJavaVersion();

		if(version == null)
			throw new RuntimeException("can not acquire installed java version");

		if(REQ_JAVA_VERSION.compareToIgnoreCase(version) > 0)
			throw new RuntimeException(String.format("java version %s or above is required", REQ_JAVA_VERSION));
	}

	private static boolean hasConsole()
	{
		return CONSOLE != null;
	}

	private static void releaseLock()
	{
		if(lock != null)
		{
			try {lock.release(); lock.channel().close();}
			catch (IOException e) {}
		}
	}

	private static String parseArgs(String[] args)
	{
		if(args.length == 0)
			return getDefaultConfigFile();
		else if(args.length == 1 && args[0].equals("-1"))
		{
			isRunOnce = true;
			return getDefaultConfigFile();
		}
		else if(args.length == 2 && args[0].equals("-f") && !args[1].equals("-1"))
			return args[1];
		else if(args.length == 3)
		{
			if(args[0].equals("-f") && args[2].equals("-1"))
			{
				isRunOnce = true;
				return args[1];
			}
			else if(args[0].equals("-1") && args[1].equals("-f"))
			{
				isRunOnce = true;
				return args[2];
			}
		}

		String msg = String.format("Invalid parameters.%s    Usage: %s", NEWLINE_CHAR, USAGE);
		throw new RuntimeException(msg);
	}

	private static void checkSingleInstance()
	{
		try
		{
			RandomAccessFile raf = new RandomAccessFile(getLockFile(), "rw");
			FileChannel fc		 = raf.getChannel();

			lock = fc.tryLock();

			if(lock != null && lock.isValid())
			{
				ByteBuffer buffer = ByteBuffer.allocate(4);
				buffer.asIntBuffer().put(currentProcessId());
				fc.write(buffer);
			}
			else
			{
				fc.close();
				raf.close();

				raf = new RandomAccessFile(getLockFile(), "r");
				MappedByteBuffer mbf = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, raf.length());
				int preInstanceId = mbf.getInt();
				raf.close();

				String msg = String.format("another instance (PID: %d) is running", preInstanceId);
				throw new RuntimeException(msg);
			}
		}
		catch(IOException e)
		{
			throw new RuntimeException("check single instance fail", e);
		}
	}

	private static void startLogCutSchedule()
	{		
		for(Runnable r : runners)
			sc.scheduleAtFixedRate(r, getStartCheckDelay(), getCheckInterval(), SCHEDULE_TIMEUNIT);
	}

	private static void loadRunners()
	{
		LogUtil.logServerStartup(LogCutter.class);
		logger.info(summary());

		if(hasDelFiles())
			runners.add(new DelFileRunner(getDelFiles()));
		if(hasCutFiles())
			runners.add(new CutFileRunner(getCutFiles()));
		if(hasArcFiles())
			runners.add(new ArcFileRunner(getArcFiles()));
		
		sc.setCorePoolSize(runners.size());
	}

	private static void sleep()
	{
		String msg;
		String name	= getAppName();
		int procId	= currentProcessId();

		if(IS_WINDOWS_PLATFORM)
			msg = String.format("%s is running as a service, use Service Manager to stop me.", name);
		else
			msg = String.format("%s is running in background, use 'kill %d' to stop me.", name, procId);

		out.println();
		out.println(msg);

		try
		{
			while(true)
			{
				if(sc.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS))
					break;
			}
		}
		catch(InterruptedException e)
		{
		}
	}

	private static void waitForDirection()
	{
		registerShutdownHook();

		if(hasConsole())
			parseDirection();
		else
			sleep();
	}
	
	private static void runOnce()
	{
		String msg = "(running-only-once mode)";
		
		out.println(msg);
		logger.info(msg);
		
		int size		= runners.size();
		Future<?>[] fs	= new Future<?>[size];
		
		for(int i = 0; i < size; i++)
			fs[i] = sc.submit(runners.get(i));
		
		for(int i = 0; i < size; i++)
		{
			try
			{
				fs[i].get();
			}
			catch(Exception e)
			{
				String action = String.format("'%s'", fs[i].getClass().getSimpleName());
				LogUtil.exception(e, action, Level.ERROR, false);
			}
		}
		
		shutdown();
	}

	private static synchronized void shutdown()
	{
		if(!hasShutdown)
		{
			hasShutdown = true;

			out.println("be about to shutdown, please wait ...");

			sc.shutdown();

			try
			{
				if(sc.awaitTermination(SHUTDOWN_AWAIT_TIME, TimeUnit.MINUTES))
					out.println("shutdown perfectly !");
				else
					out.println("shutdown imperfectly, some job DO NOT finish, please check !");
			}
			catch(InterruptedException e)
			{
				out.println("shutdown waiting had been interrupted, may be any error occur, please check !");
			}

			LogUtil.logServerShutdown(LogCutter.class);
		}
	}

	private static void registerShutdownHook()
	{
		final Thread main = Thread.currentThread();

		Runtime.getRuntime().addShutdownHook
		(
			new Thread()
			{
				@Override
				public void run()
				{
					if(!hasShutdown)
					{
						out.println();

						if(!hasConsole())
						{
							out.println(String.format("!! %s received terminate signal !!", getAppName()));
							main.interrupt();
						}

						shutdown();
					}
				}
			}
		);
	}

	private static void parseDirection()
	{
		printDirectionHelp();

		while(true)
		{
			out.print("> ");

			String input = CONSOLE.readLine();

			if(input == null)
			{
				if(hasShutdown)
					break;
				else
				{
					out.println();
					continue;
				}
			}

			if(input.equalsIgnoreCase(SHUTDOWN_DOWN_KEY))
			{
				shutdown();
				break;
			}
			else if(input.equalsIgnoreCase(SHOW_SUMMARY_KEY))
				out.println(summary());
			else if(input.equalsIgnoreCase(SHOW_HELP_KEY))
				printDirectionHelp();
			else if(input.equalsIgnoreCase(SHOW_JOBS_KEY))
				printJobs();
			else if(input.equalsIgnoreCase(SHOW_ABOUT_KEY))
				printAbout();
			else if(input.equalsIgnoreCase(RUN_AT_ONCE_KEY))
				executeRunner();
			else if(!input.isEmpty())
				out.println("(invalid command)");
		}
	}

	private static void executeRunner()
	{
		for(Runnable r : runners)
			sc.execute(r);

		out.println("manual jobs are scheduled !");
	}

	private static void printDirectionHelp()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("command line usage ")																.append(NEWLINE_CHAR);
		sb.append("------------------------------------------------------------")						.append(NEWLINE_CHAR);
		sb.append(String.format("%8s : ", SHOW_HELP_KEY))		.append("Show help")					.append(NEWLINE_CHAR);
		sb.append(String.format("%8s : ", SHOW_JOBS_KEY))		.append("Show jobs status")				.append(NEWLINE_CHAR);
		sb.append(String.format("%8s : ", SHOW_SUMMARY_KEY))	.append("Show configuration summary")	.append(NEWLINE_CHAR);
		sb.append(String.format("%8s : ", RUN_AT_ONCE_KEY))		.append("Schedule jobs manually")		.append(NEWLINE_CHAR);
		sb.append(String.format("%8s : ", SHUTDOWN_DOWN_KEY))	.append("Shutdown application")			.append(NEWLINE_CHAR);
		sb.append(String.format("%8s : ", SHOW_ABOUT_KEY))		.append("About me")						.append(NEWLINE_CHAR);
		sb.append("------------------------------------------------------------")						.append(NEWLINE_CHAR);

		out.print(sb.toString());
	}

	private static void printJobs()
	{
		int total			= runners.size();
		StringBuilder sb	= new StringBuilder();

		sb.append(String.format("jobs summary (total: %d, active: %d) ", total, sc.getActiveCount()))	.append(NEWLINE_CHAR);
		sb.append("------------------------------------------------------------")						.append(NEWLINE_CHAR);

		for(int i = 0; i < total; ++i)
		{
			FileRunner r	= runners.get(i);
			String status	= r.isRunning() ? "Active" : " Idle ";

			sb.append(String.format("%5d. %-39s%4s[ %s ]", i + 1, r, "", status))						.append(NEWLINE_CHAR);
		}

		sb.append("------------------------------------------------------------")						.append(NEWLINE_CHAR);

		out.print(sb.toString());
	}

	private static void printAbout()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("%s %s - JessMA Open Source, all rights reserved. ", getAppName(), getAppVersion()))								.append(NEWLINE_CHAR);
		sb.append("------------------------------------------------------------")																	.append(NEWLINE_CHAR);
		sb.append(String.format("%15s : ", "Description"))	.append("schedule to DELETE, CUT and ARCHIVE text log files automatically or manually.").append(NEWLINE_CHAR);
		sb.append(String.format("%15s : ", "Support"))		.append("GB18030, UTF-8, UTF-16LE and UTF-16BE text file types.")						.append(NEWLINE_CHAR);
		sb.append(String.format("%15s : ", "Usage"))		.append(USAGE)																			.append(NEWLINE_CHAR);
		sb.append(String.format("%15s   ", ""))				.append(String.format("(default config file is '%s')", getDefaultConfigFile()))			.append(NEWLINE_CHAR);
		sb.append("------------------------------------------------------------")																	.append(NEWLINE_CHAR);

		out.print(sb.toString());
	}
}
