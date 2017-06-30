package org.eclipse.hono.analysis.action.pojo;

import java.lang.reflect.Type;

import org.eclipse.hono.analysis.action.Application;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ActionElementAdapter
    implements JsonSerializer<BaseAnalysisElement>, JsonDeserializer<BaseAnalysisElement> {

  @Override
  public BaseAnalysisElement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();
    JsonPrimitive typeAsJsonPrimitive = jsonObject.getAsJsonPrimitive("type");
    if (typeAsJsonPrimitive == null || typeAsJsonPrimitive.getAsString() == null) {
      return context.deserialize(json, typeOfT);
    }
    String type = typeAsJsonPrimitive.getAsString();
    if (type.equalsIgnoreCase(Application.ACTION_TYPE_WEBHOOK)) {
      return context.deserialize(json, WebHook.class);
    } else if (type.equalsIgnoreCase(Application.ACTION_TYPE_MAIL)) {
      return context.deserialize(json, Mail.class);
    } else {
      return context.deserialize(json, Action.class);
    }
  }

  @Override
  public JsonElement serialize(BaseAnalysisElement src, Type typeOfSrc, JsonSerializationContext context) {
    return context.serialize(src, src.getClass());
  }

}
