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

package org.jessma.logcutter.runner;


import java.io.File;
import java.io.FileFilter;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.jessma.logcutter.global.FilePath;
import org.jessma.logcutter.util.GeneralHelper;
import org.jessma.logcutter.util.LogUtil;

public abstract class FileRunner implements Runnable
{
	private static final Logger logger = LogUtil.getDefaultLogger();
	
	private boolean running;
	protected final List<? extends FilePath> files;

	public FileRunner(List<? extends FilePath> files)
	{
		this.files = files;
	}

	abstract protected FileFilter getFileFilter(final FilePath fp);
	abstract protected void process(File file, final FilePath fp);

	public boolean isRunning()
	{
		return running;
	}

	@Override
	synchronized public void run()
	{
		markStart();

		for(FilePath fp : files)
		{
			try
			{
    			File path = new File(fp.getPath());

    			if(path.isDirectory())
    			{
    				FileFilter ff	= getFileFilter(fp);
    				File[] fs		= path.listFiles(ff);

    				for(File f : fs)
    					process(f, fp);
    			}
			}
			catch(Exception e)
			{
				LogUtil.exception(e, toString(), Level.ERROR, true);
			}
		}

		markEnd();
	}

	private void markStart()
	{
		logger.info(String.format("- - - - - - - -> start %13s <- - - - - - - -", this.getClass().getSimpleName()));
		running = true;
	}

	private void markEnd()
	{
		logger.info(String.format("- - - - - - - ->   end %13s <- - - - - - - -", this.getClass().getSimpleName()));
		running = false;
	}

	protected abstract static class FileFilterBase extends GeneralHelper.FileNameFileFilter
	{
		public FileFilterBase(String name)
		{
			super(name);
		}

		@Override
		public boolean accept(File file)
		{
			if(super.accept(file))
			{
				if(file.canWrite())
					return doAccept(file);
				else
					logger.warn("'{}' can not be written", file.getAbsolutePath());
			}

			return false;
		}

		protected abstract boolean doAccept(File file);
	}
	
	protected static boolean deleteFile(File file)
	{
		if(!file.exists())
			return false;
		
		if(file.isDirectory())
		{
			File[] files = file.listFiles();
			
			for(File f : files)
				deleteFile(f);
		}
		
		boolean result	= file.delete();
		String path		= file.getAbsolutePath();
		
		if(result == true)
			logger.info("    <Delete  OK > : {}", path);
		else
			logger.warn("    <Delete FAIL> : {}", path);
		
		return result;
	}

	@Override
	public String toString()
	{
		return String.format("%s@%s", getClass().getSimpleName(), hashCode());
	}
}
