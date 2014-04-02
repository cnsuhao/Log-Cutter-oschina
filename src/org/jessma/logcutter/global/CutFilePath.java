package org.jessma.logcutter.global;

public class CutFilePath extends FilePath
{
	private long threshold;
	private long reserve;

	public CutFilePath(long threshold, long reserve)
	{
		this.threshold	= threshold;
		this.reserve	= reserve;
	}

	public long getThreshold()
	{
		return threshold;
	}

	public void setThreshold(long threshold)
	{
		this.threshold = threshold;
	}

	public long getReserve()
	{
		return reserve;
	}

	public void setReserve(long reserve)
	{
		this.reserve = reserve;
	}

}
