package org.jessma.logcutter.global;

public class DelFilePath extends FilePath
{
	private long expire;

	public DelFilePath(long expire)
	{
		this.expire = expire;
	}

	public long getExpire()
	{
		return expire;
	}

	public void setExpire(long expire)
	{
		this.expire = expire;
	}

}
