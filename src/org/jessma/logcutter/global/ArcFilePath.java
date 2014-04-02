package org.jessma.logcutter.global;

public class ArcFilePath extends FilePath
{
	private long expire;
	private String archivePath;

	public ArcFilePath(long expire, String archivePath)
	{
		this.expire		 = expire;
		this.archivePath = archivePath;
	}

	public long getExpire()
	{
		return expire;
	}

	public void setExpire(long expire)
	{
		this.expire = expire;
	}

	public String getArchivePath()
	{
		return archivePath;
	}

	public void setArchivePath(String archivePath)
	{
		this.archivePath = archivePath;
	}

}
