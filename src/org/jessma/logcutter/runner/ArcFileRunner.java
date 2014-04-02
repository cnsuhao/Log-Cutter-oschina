/*
 * Copyright Bruce Liang (ldcsaa@gmail.com)
 *
 * Version	: Log-Cutter 2.0.1
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
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jessma.logcutter.global.ArcFilePath;
import org.jessma.logcutter.global.FilePath;
import org.jessma.logcutter.util.Logger;
import org.jessma.logcutter.util.archive.Zipper;

public class ArcFileRunner extends FileRunner
{
	private static final Logger logger = Logger.getDefaultLogger();
	
	public ArcFileRunner(List<ArcFilePath> files)
	{
		super(files);
	}

	@Override
	protected FileFilter getFileFilter(final FilePath fp)
	{
		return new FileFilterBase(fp.getName())
		{
			@Override
			protected boolean doAccept(File file)
			{
				long now	= new Date().getTime();
				long last	= file.lastModified();
				long days	= TimeUnit.MILLISECONDS.toDays(now - last);

				return days >= ((ArcFilePath)fp).getExpire();
			}
		};
	}

	@Override
	protected void process(File file, final FilePath fp)
	{
		try
		{
			logger.info(String.format("archiving '%s' ...", file.getAbsolutePath()));

			String target = String.format(	"%s%s_%013d%s",
											((ArcFilePath)fp).getArchivePath(),
											file.getName(),
											System.currentTimeMillis(),
											Zipper.SUFFIX);
			
			Zipper zipper = new Zipper(file.getAbsolutePath(), target);
			
			if(zipper.execute())
			{
				if(deleteFile(file))
					logger.info("OK !");
				else
					logger.warn("archive success, but can not delete source file !");
			}
			else
				logger.info("FAIL !");
		}
		catch(Exception e)
		{
			logger.error(String.format("Exception -> %s", e));
		}
	}
}
