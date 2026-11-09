package com.nationvk.galaxy

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class GalaxyApiProfile(val id:String,val name:String,val baseUrl:String,val model:String,val enabled:Boolean,val priority:Int,val hasKey:Boolean)
private object GalaxyProfileCrypto {
 fun alias(id:String)="galaxy_api_profile_"+id
 private fun secret(alias:String):SecretKey {
  val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
  (ks.getKey(alias,null) as? SecretKey)?.let{return it}
  val kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
  kg.init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
  return kg.generateKey()
 }
 fun encrypt(value:String,alias:String):String {val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,secret(alias));return Base64.encodeToString(c.iv+c.doFinal(value.toByteArray(Charsets.UTF_8)),Base64.NO_WRAP)}
 fun decrypt(value:String,alias:String):String {val b=Base64.decode(value,Base64.NO_WRAP);require(b.size>12);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,secret(alias),GCMParameterSpec(128,b.copyOfRange(0,12)));return String(c.doFinal(b.copyOfRange(12,b.size)),Charsets.UTF_8)}
 fun delete(alias:String){runCatching{KeyStore.getInstance("AndroidKeyStore").apply{load(null)}.deleteEntry(alias)}}
}
object GalaxyApiProfiles {
 private const val PREF="galaxy_api_profiles"
 private const val KEY="items"
 private const val MAX_PROFILES=500
 private fun data(c:Context)=runCatching{JSONArray(c.getSharedPreferences(PREF,0).getString(KEY,"[]")?:"[]")}.getOrDefault(JSONArray())
 fun list(c:Context):List<GalaxyApiProfile>{val a=data(c);val out=mutableListOf<GalaxyApiProfile>();for(i in 0 until a.length()){val o=a.optJSONObject(i)?:continue;out.add(GalaxyApiProfile(o.optString("id"),o.optString("name"),o.optString("url"),o.optString("model"),o.optBoolean("enabled",true),o.optInt("priority",i),o.optString("secret").isNotBlank()))};return out.sortedBy{it.priority}}
 @Synchronized fun save(c:Context,id:String?,name:String,url:String,model:String,key:String):Boolean{
  val n=name.trim().take(50);val u=url.trim().trimEnd('/');val m=model.trim()
  if(n.isBlank()||!GalaxyAiProviderStore.isSafeHttpsBaseUrl(u)||m.isBlank()||m.length>120)return false
  val a=data(c);val ix=if(id==null)-1 else (0 until a.length()).firstOrNull{a.optJSONObject(it)?.optString("id")==id}?:-1
  if(ix<0 && a.length()>=MAX_PROFILES)return false
  val pid=id?:UUID.randomUUID().toString();val old=if(ix>=0)a.optJSONObject(ix)else null
  val secret=if(key.isNotBlank())GalaxyProfileCrypto.encrypt(key.trim(),GalaxyProfileCrypto.alias(pid))else old?.optString("secret").orEmpty()
  if(secret.isBlank())return false
  val item=JSONObject().put("id",pid).put("name",n).put("url",u).put("model",m).put("secret",secret)
   .put("enabled",old?.optBoolean("enabled",true)?:true)
   .put("priority",if(ix>=0)old?.optInt("priority",ix)?:ix else 0)
  if(ix>=0) a.put(ix,item) else {
   // Preserve existing primary; append newly added profiles as backups.
   item.put("priority",a.length())
   a.put(item)
  }
  return c.getSharedPreferences(PREF,0).edit().putString(KEY,a.toString()).commit()
 }
 @Synchronized fun enable(c:Context,id:String,on:Boolean){val a=data(c);for(i in 0 until a.length()){val o=a.optJSONObject(i)?:continue;if(o.optString("id")==id){o.put("enabled",on);break}};c.getSharedPreferences(PREF,0).edit().putString(KEY,a.toString()).apply()}
 @Synchronized fun primary(c:Context,id:String){val a=data(c);val ix=(0 until a.length()).firstOrNull{a.optJSONObject(it)?.optString("id")==id}?:return;val b=JSONArray();b.put(a.getJSONObject(ix).put("priority",0));var p=1;for(i in 0 until a.length())if(i!=ix)b.put(a.getJSONObject(i).put("priority",p++));c.getSharedPreferences(PREF,0).edit().putString(KEY,b.toString()).apply()}
 @Synchronized fun delete(c:Context,id:String){val a=data(c);val b=JSONArray();for(i in 0 until a.length()){val o=a.optJSONObject(i)?:continue;if(o.optString("id")!=id)b.put(o)};GalaxyProfileCrypto.delete(GalaxyProfileCrypto.alias(id));c.getSharedPreferences(PREF,0).edit().putString(KEY,b.toString()).apply()}
 internal fun key(c:Context,id:String):String?{val a=data(c);for(i in 0 until a.length()){val o=a.optJSONObject(i)?:continue;if(o.optString("id")==id)return runCatching{GalaxyProfileCrypto.decrypt(o.optString("secret"),GalaxyProfileCrypto.alias(id))}.getOrNull()};return null}
 internal fun exportForVault(c:Context):String {
  val out=JSONArray()
  list(c).forEach { p ->
   val secret=key(c,p.id)?:throw IllegalStateException("Provider key unavailable: "+p.name)
   out.put(JSONObject().put("id",p.id).put("name",p.name).put("url",p.baseUrl)
    .put("model",p.model).put("apiKey",secret).put("enabled",p.enabled).put("priority",p.priority))
  }
  return JSONObject().put("schema","galaxy-api-profiles-v1").put("profiles",out).toString()
 }
 @Synchronized internal fun importFromVault(c:Context,json:String) {
  val root=JSONObject(json)
  require(root.optString("schema")=="galaxy-api-profiles-v1") { "Unsupported provider backup format." }
  val items=root.optJSONArray("profiles")?:throw IllegalArgumentException("Provider list missing.")
  require(items.length()<=MAX_PROFILES) { "Backup contains too many provider profiles." }
  val restored=JSONArray()
  val ids=mutableSetOf<String>()
  for(i in 0 until items.length()) {
   val p=items.optJSONObject(i)?:throw IllegalArgumentException("Invalid provider item.")
   val id=p.optString("id").take(80)
   val name=p.optString("name").trim().take(50)
   val url=p.optString("url").trim().trimEnd('/')
   val model=p.optString("model").trim().take(120)
   val secret=p.optString("apiKey")
   require(id.isNotBlank() && ids.add(id) && name.isNotBlank() && model.isNotBlank() &&
    GalaxyAiProviderStore.isSafeHttpsBaseUrl(url) && secret.isNotBlank() && secret.length<=500) {
    "Invalid provider profile in backup."
   }
   val encrypted=GalaxyProfileCrypto.encrypt(secret,GalaxyProfileCrypto.alias(id))
   restored.put(JSONObject().put("id",id).put("name",name).put("url",url).put("model",model)
    .put("secret",encrypted).put("enabled",p.optBoolean("enabled",true)).put("priority",p.optInt("priority",i)))
  }
  val old=data(c)
  check(c.getSharedPreferences(PREF,0).edit().putString(KEY,restored.toString()).commit()) {
   "Could not save restored provider profiles."
  }
  for(i in 0 until old.length()) {
   val oldId=old.optJSONObject(i)?.optString("id").orEmpty()
   if(oldId.isNotBlank() && oldId !in ids) GalaxyProfileCrypto.delete(GalaxyProfileCrypto.alias(oldId))
  }
 }
 fun configureFromBuildConfig(c:Context) { /* Keys are added in-app and encrypted with Android Keystore. */ }
 fun hasEnabled(c:Context)=list(c).any{it.enabled&&it.hasKey}
}
object GalaxyMultiApiClient {
 private val worker=Executors.newSingleThreadExecutor()
 private val main=Handler(Looper.getMainLooper())
 fun send(c:Context,systemContext:String,done:(Result<String>)->Unit)=send(c,systemContext,"Please respond to the current request.",done)
 fun send(c:Context,systemContext:String,userMessage:String,done:(Result<String>)->Unit) {
  worker.execute { val result=runCatching { run(c.applicationContext,systemContext,userMessage) }; main.post { done(result) } }
 }
 private fun run(c:Context,system:String,user:String):String {
  require(user.isNotBlank()&&user.length<=12000) { "User message is empty or too large." }
  require(system.length<=24000) { "Conversation context is too large." }
  val profiles=GalaxyApiProfiles.list(c).filter{it.enabled&&it.hasKey};val errors=mutableListOf<String>()
  for(p in profiles) try { val k=GalaxyApiProfiles.key(c,p.id)?:throw IllegalStateException("Saved key unavailable");return request(p.baseUrl,p.model,k,system,user) }
  catch(e:Exception){errors.add(p.name+": "+(e.message?:"failed"))}
  if(profiles.isEmpty()&&GalaxyAiProviderStore.load(c).isConfigured) {
   val old=GalaxyAiProviderStore.load(c);val k=GalaxyAiProviderStore.readApiKey(c)?:throw IllegalStateException("Saved API key unavailable")
   return request(old.baseUrl,old.model,k,system,user)
  }
  if(errors.isEmpty()) throw IllegalStateException("No enabled API profile. Add and enable a provider in Settings.")
  throw IllegalStateException("All API profiles failed: "+errors.joinToString(" | ").take(700))
 }
 private fun request(baseUrl:String,model:String,key:String,system:String,user:String):String {
  val host=java.net.URI(baseUrl).host.orEmpty()
  return if(host.equals("generativelanguage.googleapis.com",true)) gemini(baseUrl,model,key,system,user)
  else compatible(baseUrl,model,key,system,user)
 }
 private fun compatible(baseUrl:String,model:String,key:String,system:String,user:String):String {
  val base=baseUrl.trimEnd('/');val endpoint=if(base.endsWith("/v1",true)) "$base/chat/completions" else "$base/v1/chat/completions"
  val messages=JSONArray();if(system.isNotBlank())messages.put(JSONObject().put("role","system").put("content",system))
  messages.put(JSONObject().put("role","user").put("content",user))
  val body=JSONObject().put("model",model).put("temperature",0.7).put("max_tokens",1200).put("messages",messages)
  val con=(URL(endpoint).openConnection() as HttpURLConnection).apply {
   requestMethod="POST";connectTimeout=15000;readTimeout=45000;doOutput=true;instanceFollowRedirects=false
   setRequestProperty("Authorization","Bearer $key");setRequestProperty("Content-Type","application/json; charset=utf-8");setRequestProperty("Accept","application/json")
  }
  return try {
   con.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}
   val code=con.responseCode;val raw=(if(code in 200..299)con.inputStream else con.errorStream)?.bufferedReader(Charsets.UTF_8)?.use{it.readText().take(1000000)}.orEmpty()
   if(code !in 200..299){val msg=runCatching{JSONObject(raw).optJSONObject("error")?.optString("message")}.getOrNull();throw IllegalStateException("HTTP $code"+(if(msg.isNullOrBlank())"" else ": "+msg.take(180)))}
   val answer=JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty().trim()
   if(answer.isBlank())throw IllegalStateException("Provider returned an empty response.");answer.take(30000)
  } finally {con.disconnect()}
 }
 private fun gemini(baseUrl:String,model:String,key:String,system:String,user:String):String {
  require(model.matches(Regex("[A-Za-z0-9._-]{1,120}"))){"Invalid Gemini model ID."}
  val base=baseUrl.trimEnd('/');val versioned=if(base.endsWith("/v1beta",true))base else "$base/v1beta"
  val body=JSONObject().put("contents",JSONArray().put(JSONObject().put("role","user").put("parts",JSONArray().put(JSONObject().put("text",user)))))
   .put("generationConfig",JSONObject().put("temperature",0.7).put("maxOutputTokens",1200))
  if(system.isNotBlank())body.put("systemInstruction",JSONObject().put("parts",JSONArray().put(JSONObject().put("text",system))))
  val con=(URL("$versioned/models/$model:generateContent").openConnection() as HttpURLConnection).apply {
   requestMethod="POST";connectTimeout=15000;readTimeout=45000;doOutput=true;instanceFollowRedirects=false
   setRequestProperty("x-goog-api-key",key);setRequestProperty("Content-Type","application/json; charset=utf-8");setRequestProperty("Accept","application/json")
  }
  return try {
   con.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}
   val code=con.responseCode;val raw=(if(code in 200..299)con.inputStream else con.errorStream)?.bufferedReader(Charsets.UTF_8)?.use{it.readText().take(1000000)}.orEmpty()
   if(code !in 200..299){val msg=runCatching{JSONObject(raw).optJSONObject("error")?.optString("message")}.getOrNull();throw IllegalStateException("Gemini HTTP $code"+(if(msg.isNullOrBlank())"" else ": "+msg.take(180)))}
   val parts=JSONObject(raw).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
   val answer=buildString{if(parts!=null)for(x in 0 until parts.length())append(parts.optJSONObject(x)?.optString("text").orEmpty())}.trim()
   if(answer.isBlank())throw IllegalStateException("Gemini returned an empty response.");answer.take(30000)
  } finally {con.disconnect()}
 }
}