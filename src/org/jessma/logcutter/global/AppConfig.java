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

package org.jessma.logcutter.global;


import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jessma.logcutter.util.GeneralHelper;

import static org.jessma.logcutter.util.GeneralHelper.NEWLINE_CHAR;
import static org.jessma.logcutter.util.GeneralHelper.isStrEmpty;
import static org.jessma.logcutter.util.GeneralHelper.isStrNotEmpty;
import static org.jessma.logcutter.util.GeneralHelper.str2Long;


public class AppConfig
{
	private static final String APP_NAME				= "LogCutter";
	private static final String APP_VERSION				= "2.0.2";
	private static final String CLASS_PATH				= GeneralHelper.getClassResourcePath(AppConfig.class, "/");
	private static final int PROCESS_ID					= GeneralHelper.getProcessId();

	private static final String DEF_CONF_FILE			= CLASS_PATH + "../conf/config.xml";
	private static final String DEF_LOG4J_CONF_FILE		= CLASS_PATH + "../conf/log4j2.xml";
	private static final String DEF_LOCK_FILE			= CLASS_PATH + "../" + APP_NAME + ".lock";

	private static final long DEF_START_CHK_DELAY		= 0L * 60;
	private static final long DEF_CHK_INTERVAL			= 72L * 60;
	private static final long DEF_DEL_FILES_EXPIRE		= 90L;
	private static final long DEF_CUT_FILES_THRESHOLD	= 10240L;
	private static final long DEF_CUT_FILES_RESERVE		= 1024L;
	private static final long DEF_ARC_FILES_EXPIRE		= 90L;

	private static String log4jConfigFile	= DEF_LOG4J_CONF_FILE;
	private static String lockFile			= DEF_LOCK_FILE;

	private static long startCheckDelay		= DEF_START_CHK_DELAY;
	private static long checkInterval		= DEF_CHK_INTERVAL;

	private static List<DelFilePath> delFiles	= new ArrayList<DelFilePath>();
	private static List<CutFilePath> cutFiles	= new ArrayList<CutFilePath>();
	private static List<ArcFilePath> arcFiles	= new ArrayList<ArcFilePath>();

	public static final String getAppName()
	{
		return APP_NAME;
	}

	public static final String getAppVersion()
	{
		return APP_VERSION;
	}

	public static final int currentProcessId()
	{
		return PROCESS_ID;
	}

	public static final String getClassPath()
	{
		return CLASS_PATH;
	}

	public static final String getDefaultConfigFile()
	{
		return DEF_CONF_FILE;
	}

	public static final String getLog4jConfigFile()
	{
		return log4jConfigFile;
	}

	public static final String getLockFile()
	{
		return lockFile;
	}

	public static final long getStartCheckDelay()
	{
		return startCheckDelay;
	}

	public static final long getCheckInterval()
	{
		return checkInterval;
	}

	public static final List<DelFilePath> getDelFiles()
	{
		return delFiles;
	}

	public static final boolean hasDelFiles()
	{
		return !delFiles.isEmpty();
	}

	public static final List<CutFilePath> getCutFiles()
	{
		return cutFiles;
	}

	public static final boolean hasCutFiles()
	{
		return !cutFiles.isEmpty();
	}

	public static final List<ArcFilePath> getArcFiles()
	{
		return arcFiles;
	}

	public static final boolean hasArcFiles()
	{
		return !arcFiles.isEmpty();
	}

	@SuppressWarnings("unchecked")
	public static final void init(String file)
	{
		try
		{
			SAXReader sr = new SAXReader();
    		Document doc = sr.read(new File(file));
    		Element root = doc.getRootElement();

    		// 加载应用程序的配置信息 ...

    		// <global>
    		Element global = root.element("global");
    		parseGlobal(global);

    		// <delete-files>
    		List<Element> dfs = root.elements("delete-files");
    		parseDelFiles(dfs);

    		// <cut-files>
    		List<Element> cfs = root.elements("cut-files");
    		parseCutFiles(cfs);

       		// <archive-files>
    		List<Element> afs = root.elements("archive-files");
    		parseArcFiles(afs);

    		if(!hasDelFiles() && !hasCutFiles() && !hasArcFiles())
    			throw new RuntimeException("none of 'delete-files' / 'cut-files' / 'archive-files' found");
		}
		catch(Exception e)
		{
			throw new RuntimeException("load application configuration fail", e);
		}
	}

	private static void parseGlobal(Element global)
	{
		if(global != null)
		{
			// <start-check-delay>
			Element chkDelay = global.element("start-check-delay");
			if(chkDelay != null)
			{
				String value	= chkDelay.getTextTrim();
				startCheckDelay	= parseDelay(value);
				
				if(startCheckDelay < 0)
				{
					startCheckDelay = str2Long(value, -1);
					if(startCheckDelay < 0)
						startCheckDelay = DEF_START_CHK_DELAY;
					else
						startCheckDelay *= 60;
				}
			}

			// <check-interval>
			Element chkIntv = global.element("check-interval");
			if(chkIntv != null)
			{
				checkInterval = str2Long(chkIntv.getTextTrim(), -1);
				if(checkInterval <= 0)
					checkInterval = DEF_CHK_INTERVAL;
				else
					checkInterval *= 60;
			}

			// <log4j-config-file>
			Element log4jCfg = global.element("log4j-config-file");
			if(log4jCfg != null)
			{
				String config = log4jCfg.getTextTrim();
				if(isStrNotEmpty(config))
					log4jConfigFile = config;
			}

			// <lock-file>
			Element lcFile = global.element("lock-file");
			if(lcFile != null)
			{
				String lock = lcFile.getTextTrim();
				if(isStrNotEmpty(lock))
					lockFile = lock;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void parseDelFiles(List<Element> dfs)
	{
		for(Element e : dfs)
		{
			long delFilesExpire = DEF_DEL_FILES_EXPIRE;
			
			// <delete-files.expire>
			Attribute exp = e.attribute("expire");
			if(exp != null)
			{
				delFilesExpire = str2Long(exp.getValue(), -1);
				if(delFilesExpire <= 0)
					delFilesExpire = DEF_DEL_FILES_EXPIRE;
			}

			// <file>
			List<Element> fs = e.elements("file");
			for(Element f : fs)
			{
				DelFilePath fp = new DelFilePath(delFilesExpire);
				
				parseFilePath(f, fp);
				delFiles.add(fp);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void parseCutFiles(List<Element> cfs)
	{
		for(Element e : cfs)
		{
			long cutFilesThreshold	= DEF_CUT_FILES_THRESHOLD;
			long cutFilesReserve	= DEF_CUT_FILES_RESERVE;
			
			// <cut-files.threshold>
			Attribute threshold = e.attribute("threshold");
			if(threshold != null)
			{
				cutFilesThreshold = str2Long(threshold.getValue(), -1);
				if(cutFilesThreshold <= 0)
					cutFilesThreshold = DEF_CUT_FILES_THRESHOLD;
			}

			// <cut-files.reserve>
			Attribute reserve = e.attribute("reserve");
			if(reserve != null)
			{
				cutFilesReserve = str2Long(reserve.getValue(), -1);
				if(cutFilesReserve < 0)
					cutFilesReserve = DEF_CUT_FILES_RESERVE;
			}

			if(cutFilesThreshold <= cutFilesReserve)
				throw new RuntimeException("'cut-files.threshold' must greater then 'cut-files.reserve'");

			// <file>
			List<Element> fs = e.elements("file");
			for(Element f : fs)
			{
				CutFilePath fp = new CutFilePath(cutFilesThreshold, cutFilesReserve);
				
				parseFilePath(f, fp);
				cutFiles.add(fp);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void parseArcFiles(List<Element> afs)
	{
		for(Element e : afs)
		{
			long arcFilesExpire	= DEF_ARC_FILES_EXPIRE;
			String arcPath		= null;
			
			// <archive-files.expire>
			Attribute exp = e.attribute("expire");
			if(exp != null)
			{
				arcFilesExpire = str2Long(exp.getValue(), -1);
				if(arcFilesExpire <= 0)
					arcFilesExpire = DEF_ARC_FILES_EXPIRE;
			}
			
			// <archive-files.archive-path>
			Attribute ap = e.attribute("archive-path");
			if(ap != null)
			{
				arcPath = ap.getValue();
				if(isStrEmpty(arcPath))
					throw new RuntimeException("'archive-files.archive-path' attribute must not empty");
				
				if(!arcPath.endsWith(File.separator))
					arcPath = arcPath + File.separator;
			}
			else
				throw new RuntimeException("'archive-files.archive-path' attribute must be set");

			// <file>
			List<Element> fs = e.elements("file");
			for(Element f : fs)
			{
				ArcFilePath fp = new ArcFilePath(arcFilesExpire, arcPath);
				
				parseFilePath(f, fp);
				arcFiles.add(fp);
			}
		}
	}

	private static void parseFilePath(Element file, FilePath fp)
	{
		Attribute p = file.attribute("path");

		if(p == null)
			throw new RuntimeException("'file' element must have 'path' attribute");

		String path = p.getValue();
		if(isStrEmpty(path))
			throw new RuntimeException("'file.path' attribute must not empty");
		
		if(!path.endsWith(File.separator))
			path = path + File.separator;

		String name = file.getTextTrim();
		if(isStrEmpty(name))
			throw new RuntimeException("'file' element must not empty");
		
		fp.setPath(path);
		fp.setName(name);
	}

	public static final String summary()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("configuration summary ")													.append(NEWLINE_CHAR);
		sb.append("------------------------------------------------------------")			.append(NEWLINE_CHAR);

		sb.append("[global]")																.append(NEWLINE_CHAR);
		sb.append(String.format("%21s : %-5d minutes", "start-check-delay", startCheckDelay)).append(NEWLINE_CHAR);
		sb.append(String.format("%21s : %-5d minutes", "check-interval", checkInterval))	.append(NEWLINE_CHAR);
		sb.append(String.format("%21s : %s", "log4j-config-file", log4jConfigFile))			.append(NEWLINE_CHAR);
		sb.append(String.format("%21s : %s", "lock-file", lockFile))						.append(NEWLINE_CHAR);

		if(hasDelFiles())
		{
			long expire = -1;
			
			for(int i = 0; i < delFiles.size(); ++i)
			{
				DelFilePath dfp = delFiles.get(i);
				
				if(dfp.getExpire() != expire)
				{
					expire = dfp.getExpire();
					
					sb.append(String.format("[delete-files] (expire: %d days)", expire))	.append(NEWLINE_CHAR);
				}
				
				sb.append(String.format("%5d. %s", i + 1, dfp))								.append(NEWLINE_CHAR);
			}
		}
		else
		{
			sb.append("[delete-files]")														.append(NEWLINE_CHAR);
			sb.append(String.format("%10s", "(none)"))										.append(NEWLINE_CHAR);
		}

		if(hasCutFiles())
		{
			long threshold	= -1;
			long reserve	= -1;
			
			for(int i = 0; i < cutFiles.size(); ++i)
			{
				CutFilePath cfp = cutFiles.get(i);
				
				if(cfp.getThreshold() != threshold || cfp.getReserve() != reserve)
				{
					threshold	= cfp.getThreshold();
					reserve		= cfp.getReserve();
					
					sb.append(String.format("[cut-files] (threshold: %d KBs, reserve: %d KBs)", threshold, reserve))
																							.append(NEWLINE_CHAR);
				}
				
				sb.append(String.format("%5d. %s", i + 1, cfp))								.append(NEWLINE_CHAR);
			}
		}
		else
		{
			sb.append("[cut-files]")														.append(NEWLINE_CHAR);
			sb.append(String.format("%10s", "(none)"))										.append(NEWLINE_CHAR);
		}

		if(hasArcFiles())
		{
			long expire		= -1;
			String path		= null;
			
			for(int i = 0; i < arcFiles.size(); ++i)
			{
				ArcFilePath afp = arcFiles.get(i);
				
				if(afp.getExpire() != expire || !afp.getArchivePath().equals(path))
				{
					expire	= afp.getExpire();
					path	= afp.getArchivePath();
					
					sb.append(String.format("[archive-files] (expire: %d days, archive-path: '%s')", expire, path))
																							.append(NEWLINE_CHAR);
				}
				
				sb.append(String.format("%5d. %s", i + 1, afp))								.append(NEWLINE_CHAR);
			}
		}
		else
		{
			sb.append("[archive-files]")													.append(NEWLINE_CHAR);
			sb.append(String.format("%10s", "(none)"))										.append(NEWLINE_CHAR);
		}

		sb.append("------------------------------------------------------------");

		return sb.toString();
	}
	
	private static final long parseDelay(String hhmm)
	{
		long ts			= -1L;
		String[] tm		= hhmm.split(":");
		Calendar delay	= null;
		Calendar now	= Calendar.getInstance();
		
		now.set(Calendar.SECOND, 0);
		now.set(Calendar.MILLISECOND, 0);
	
		if(tm.length == 2)
		{
			int hour	= GeneralHelper.str2Int(tm[0], -1);
			int minute	= GeneralHelper.str2Int(tm[1], -1);
			
			if(hour >= 0 && minute >= 0)
			{
				delay = (Calendar)now.clone();
				delay.set(Calendar.HOUR_OF_DAY, hour);
				delay.set(Calendar.MINUTE, minute);
				
				if(delay.compareTo(now) < 0)
					delay.add(Calendar.DAY_OF_MONTH, 1);
			}
		}
		
		if(delay != null)
			ts = TimeUnit.MILLISECONDS.toMinutes(delay.getTimeInMillis() - now.getTimeInMillis());
		
		return ts;
	}

}
