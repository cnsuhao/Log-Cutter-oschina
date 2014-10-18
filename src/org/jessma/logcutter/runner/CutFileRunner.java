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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.jessma.logcutter.global.Charset;
import org.jessma.logcutter.global.CutFilePath;
import org.jessma.logcutter.global.FilePath;
import org.jessma.logcutter.util.LogUtil;


public class CutFileRunner extends FileRunner
{
	private static final Logger logger			= LogUtil.getDefaultLogger();
	
	private static final long BYTE_UNIT_FACTOR	= 1024L;
	private static final int FILE_BUFFER_SIZE	= 8192;

	static
	{
		Charset.loadCharsets();
	}

	public CutFileRunner(List<CutFilePath> files)
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
				return 	file.isFile() &&
						file.length() >= ((CutFilePath)fp).getThreshold() * BYTE_UNIT_FACTOR;
			}
		};
	}

	@Override
	protected void process(File file, final FilePath fp)
	{
		FileLock lock = null;

		try
		{
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			FileChannel fc = raf.getChannel();
			lock = fc.tryLock();

			if(lock != null && lock.isValid())
			{
				final long FILE_LENGTH	= raf.length();
				final CutFilePath cfp	= (CutFilePath)fp;

				if(FILE_LENGTH >= cfp.getThreshold() * BYTE_UNIT_FACTOR)
				{
					logger.info(String.format("cutting '%s' ...", file.getAbsolutePath()));

					raf.seek(FILE_LENGTH - cfp.getReserve() * BYTE_UNIT_FACTOR);

					Charset cs				= Charset.detechCharset(raf);
					final long RESERVE_POS	= cs.scanNextLine(raf);

					cutFile(fc, cs, FILE_LENGTH, RESERVE_POS);

					logger.info("OK !");
				}
			}
			else
				logger.warn("can not lock file: '%s'", file.getAbsolutePath());
		}
		catch(Exception e)
		{
			logger.error(String.format("Exception -> %s", e));
		}
		finally
		{
			if(lock != null)
			{
				try {lock.release(); lock.channel().close();}
				catch (IOException e) {}
			}
		}
	}

	private void cutFile(FileChannel fc, Charset cs, final long FILE_LENGTH, final long RESERVE_POS) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(FILE_BUFFER_SIZE);
		buffer.put(cs.getBom());
		fc.position(0);

		long tr = 0;
		for(int r = 0; (r = fc.read(buffer, RESERVE_POS + tr)) > 0; tr += r)
		{
			buffer.flip();
			fc.write(buffer);
			buffer.clear();
		}

		long reserve = cs.getBomLength() + (FILE_LENGTH - RESERVE_POS);
		fc.truncate(reserve);
	}
}
