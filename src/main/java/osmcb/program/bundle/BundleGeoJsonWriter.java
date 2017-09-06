package osmcb.program.bundle;

import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

public class BundleGeoJsonWriter implements JsonGenerator
{
	private JsonGenerator mGen = null;

	public BundleGeoJsonWriter(Writer tWriter)
	{
		mGen = Json.createGenerator(tWriter);
	}

	@Override
	public void close()
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void flush()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public JsonGenerator write(JsonValue arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator write(String arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator write(BigDecimal arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator write(BigInteger arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator write(int arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator write(long arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator write(double arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator write(boolean arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator write(String arg0, JsonValue arg1)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator write(String arg0, String arg1)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator write(String arg0, BigInteger arg1)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator write(String arg0, BigDecimal arg1)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator write(String arg0, int arg1)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator write(String arg0, long arg1)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator write(String arg0, double arg1)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator write(String arg0, boolean arg1)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator writeEnd()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator writeNull()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator writeNull(String arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator writeStartArray()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator writeStartObject()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator writeStartObject(String arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonGenerator writeStartArray(String arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
