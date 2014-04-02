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

package org.jessma.logcutter.global;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.universalchardet.Constants;
import org.mozilla.universalchardet.UniversalDetector;

public enum Charset
{
	GB18030
	{
		@Override
		protected long scan(RandomAccessFile in) throws IOException
		{
			int read = -1;
			while((read = in.read()) >= 0)
			{
				if(read == '\n')
					break;
			}

			return in.getFilePointer();
		}

		@Override
		public byte[] getBom()
		{
			return GB18030_BOM;
		}

		@Override
		public int getBomLength()
		{
			return GB18030_BOM.length;
		}

		@Override
		public String toString()
		{
			return Constants.CHARSET_GB18030;
		}
	},

	UTF_8
	{
		@Override
		public long scan(RandomAccessFile in) throws IOException
		{
			int read = -1;
			while((read = in.read()) >= 0)
			{
				if(read == '\n')
					break;
			}

			return in.getFilePointer();
		}

		@Override
		public byte[] getBom()
		{
			return UTF_8_BOM;
		}

		@Override
		public int getBomLength()
		{
			return UTF_8_BOM.length;
		}

		@Override
		public String toString()
		{
			return Constants.CHARSET_UTF_8;
		}
	},

	UTF_16BE
	{
		@Override
		public long scan(RandomAccessFile in) throws IOException
		{
			int b1 = -1, b2 = -1;
			while((b1 = in.read()) >= 0)
			{
				if(b1 == '\n' && b2 == '\0')
					break;
				else
					b2 = b1;
			}

			return in.getFilePointer();
		}

		@Override
		public byte[] getBom()
		{
			return UTF_16BE_BOM;
		}

		@Override
		public int getBomLength()
		{
			return UTF_16BE_BOM.length;
		}

		@Override
		public String toString()
		{
			return Constants.CHARSET_UTF_16BE;
		}
	},

	UTF_16LE
	{
		@Override
		public long scan(RandomAccessFile in) throws IOException
		{
			int b1 = -1, b2 = -1;
			while((b1 = in.read()) >= 0)
			{
				if(b1 == '\0' && b2 == '\n')
					break;
				else
					b2 = b1;
			}

			return in.getFilePointer();
		}

		@Override
		public byte[] getBom()
		{
			return UTF_16LE_BOM;
		}

		@Override
		public int getBomLength()
		{
			return UTF_16LE_BOM.length;
		}

		@Override
		public String toString()
		{
			return Constants.CHARSET_UTF_16LE;
		}
	};

	private static final int DET_BUFFER_SIZE	= 4096;
	private static final int DET_TOOTAL_SIZE	= DET_BUFFER_SIZE * 512;
	private static final Charset DEF_CHARSET	= UTF_8;

	public static final byte[] GB18030_BOM		= new byte[0];
	public static final byte[] UTF_8_BOM		= new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF };
	public static final byte[] UTF_16BE_BOM		= new byte[] { (byte)0xFE, (byte)0xFF };
	public static final byte[] UTF_16LE_BOM		= new byte[] { (byte)0xFF, (byte)0xFE };

	protected static final Map<String, Charset> charsetMap = new HashMap<String,  Charset>();

	protected abstract long	scan(RandomAccessFile in) throws IOException;
	public abstract byte[]	getBom();
	public abstract int		getBomLength();

	public static void loadCharsets()
	{
		charsetMap.put(null,				DEF_CHARSET);
		charsetMap.put(GB18030.toString(),	GB18030);
		charsetMap.put(UTF_8.toString(),		UTF_8);
		charsetMap.put(UTF_16BE.toString(),	UTF_16BE);
		charsetMap.put(UTF_16LE.toString(),	UTF_16LE);
	}

	public long scanNextLine(RandomAccessFile in) throws IOException
	{
		long fp	= in.getFilePointer();
		long pos	= scan(in);

		in.seek(fp);

		return pos;
	}

	public static Charset detechCharset(RandomAccessFile in) throws IOException
	{
		long filepos = in.getFilePointer();
		in.seek(0);

		byte[] buffer = new byte[DET_BUFFER_SIZE];
		UniversalDetector detector = new UniversalDetector(null);

		long tr = 0;
		for(int r = 0; !detector.isDone() && tr < DET_TOOTAL_SIZE && (r = in.read(buffer)) > 0; tr += r)
			detector.handleData(buffer, 0, r);

		detector.dataEnd();
		in.seek(filepos);

		String detCs	= detector.getDetectedCharset();
		Charset cs	= charsetMap.get(detCs);

		if(cs == null)
			cs = DEF_CHARSET;

		return cs;
	}
}
