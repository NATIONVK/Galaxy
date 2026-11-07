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
 private fun data(c:Context)=runCatching{JSONArray(c.getSharedPreferences(PREF,0).getString(KEY,"[]")?:"[]")}.getOrDefault(JSONArray())
 fun list(c:Context):List<GalaxyApiProfile>{val a=data(c);val out=mutableListOf<GalaxyApiProfile>();for(i in 0 until a.length()){val o=a.optJSONObject(i)?:continue;out.add(GalaxyApiProfile(o.optString("id"),o.optString("name"),o.optString("url"),o.optString("model"),o.optBoolean("enabled",true),o.optInt("priority",i),o.optString("secret").isNotBlank()))};return out.sortedBy{it.priority}}
 @Synchronized fun save(c:Context,id:String?,name:String,url:String,model:String,key:String):Boolean{
  val n=name.trim().take(50);val u=url.trim().trimEnd('/');val m=model.trim()
  if(n.isBlank()||!GalaxyAiProviderStore.isSafeHttpsBaseUrl(u)||m.isBlank()||m.length>120)return false
  val a=data(c);val ix=if(id==null)-1 else (0 until a.length()).firstOrNull{a.optJSONObject(it)?.optString("id")==id}?:-1
  val pid=id?:UUID.randomUUID().toString();val old=if(ix>=0)a.optJSONObject(ix)else null
  val secret=if(key.isNotBlank())GalaxyProfileCrypto.encrypt(key.trim(),GalaxyProfileCrypto.alias(pid))else old?.optString("secret").orEmpty()
  if(secret.isBlank())return false
  val item=JSONObject().put("id",pid).put("name",n).put("url",u).put("model",m).put("secret",secret).put("enabled",old?.optBoolean("enabled",true)?:true).put("priority",old?.optInt("priority",a.length())?:a.length())
  if(ix>=0)a.put(ix,item)else a.put(item)
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
  require(items.length()<=25) { "Backup contains too many provider profiles." }
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
 fun hasEnabled(c:Context)=list(c).any{it.enabled&&it.hasKey}
}
object GalaxyMultiApiClient {
 private val worker=Executors.newSingleThreadExecutor()
 private val main=Handler(Looper.getMainLooper())
 fun send(c:Context,prompt:String,done:(Result<String>)->Unit){val app=c.applicationContext;worker.execute{val r=runCatching{run(app,prompt)};main.post{done(r)}}}
 private fun run(c:Context,prompt:String):String{
  require(prompt.isNotBlank()&&prompt.length<=24000){"Conversation context is empty or too large."}
  val profiles=GalaxyApiProfiles.list(c).filter{it.enabled&&it.hasKey};val errors=mutableListOf<String>()
  for(p in profiles){try{val k=GalaxyApiProfiles.key(c,p.id)?:throw IllegalStateException("Saved key unavailable");return request(p,k,prompt)}catch(e:Exception){errors.add(p.name+": "+(e.message?:"failed"))}}
  if(profiles.isEmpty()&&GalaxyAiProviderStore.load(c).isConfigured){val old=GalaxyAiProviderStore.load(c);val k=GalaxyAiProviderStore.readApiKey(c)?:throw IllegalStateException("Saved API key unavailable");return request(old.baseUrl,old.model,k,prompt)}
  if(errors.isEmpty())throw IllegalStateException("No enabled API profile. Add and enable a provider in Settings.")
  throw IllegalStateException("All API profiles failed: "+errors.joinToString(" | ").take(700))
 }
 private fun request(p:GalaxyApiProfile,k:String,prompt:String)=request(p.baseUrl,p.model,k,prompt)
 private fun request(url:String,model:String,key:String,prompt:String):String{
  val b=url.trimEnd('/');val endpoint=if(b.endsWith("/v1",true))b+"/chat/completions" else b+"/v1/chat/completions"
  val body=JSONObject().put("model",model).put("temperature",0.7).put("max_tokens",1200).put("messages",JSONArray().put(JSONObject().put("role","system").put("content",prompt)).put(JSONObject().put("role","user").put("content","Answer the current user request in the user's language.")))
  val c=URL(endpoint).openConnection() as HttpURLConnection;c.requestMethod="POST";c.connectTimeout=15000;c.readTimeout=45000;c.doOutput=true;c.instanceFollowRedirects=false;c.setRequestProperty("Authorization","Bearer "+key);c.setRequestProperty("Content-Type","application/json; charset=utf-8");c.setRequestProperty("Accept","application/json")
  return try{c.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))};val status=c.responseCode;val stream=if(status in 200..299)c.inputStream else c.errorStream;val raw=stream?.bufferedReader(Charsets.UTF_8)?.use{it.readText().take(1000000)}.orEmpty();if(status !in 200..299){val msg=runCatching{JSONObject(raw).optJSONObject("error")?.optString("message")}.getOrNull();throw IllegalStateException("HTTP "+status+(if(msg.isNullOrBlank())"" else ": "+msg.take(160)))};val choices=JSONObject(raw).optJSONArray("choices")?:throw IllegalStateException("No response choices");val answer=choices.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty().trim();if(answer.isBlank())throw IllegalStateException("Empty response");answer.take(30000)}finally{c.disconnect()}
 }
}
