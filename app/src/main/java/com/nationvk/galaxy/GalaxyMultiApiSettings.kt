package com.nationvk.galaxy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

@Composable
fun GalaxyMultiApiSettingsPanel() {
 val context= LocalContext.current
 var refresh by remember { mutableIntStateOf(0) }
 val profiles=remember(refresh){GalaxyApiProfiles.list(context)}
 var editId by remember { mutableStateOf<String?>(null) }
 var name by remember { mutableStateOf("OpenAI") }
 var url by remember { mutableStateOf("https://api.openai.com") }
 var model by remember { mutableStateOf("gpt-4o-mini") }
 var key by remember { mutableStateOf("") }
 var status by remember { mutableStateOf("সর্বোচ্চ ২৫টি API যোগ করা যাবে। Primary ব্যর্থ হলে পরের enabled profile চেষ্টা হবে।") }
 var testing by remember { mutableStateOf(false) }
 val card=Color(0xFF101326);val field=Color(0xFF171B33);val cyan=Color(0xFF55DFFF);val violet=Color(0xFF9B69FF);val muted=Color(0xFF929AB8)
 fun reset(){editId=null;name="";url="https://api.openai.com";model="gpt-4o-mini";key=""}
 Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(card).padding(14.dp)) {
  Text("MULTI-API FAILOVER",color=cyan,fontSize=11.sp,fontWeight=FontWeight.Bold)
  Text("সর্বোচ্চ ২৫টি Provider/API Key যোগ করো। Primary ব্যর্থ হলে পরের সক্রিয় API চেষ্টা হবে। সব profile এই ডিভাইসে encrypted থাকবে।",color=muted,fontSize=10.sp,lineHeight=15.sp,modifier=Modifier.padding(top=6.dp,bottom=12.dp))
  Text("Native Gemini: URL https://generativelanguage.googleapis.com এবং Model ID যেমন gemini-2.5-flash। OpenAI-compatible API-র জন্য provider-এর HTTPS Base URL ও Model ID দাও।",color=cyan,fontSize=9.sp,lineHeight=14.sp,modifier=Modifier.padding(bottom=8.dp))
  @Composable fun field(label:String,value:String,change:(String)->Unit,secret:Boolean=false){
   Text(label,color=muted,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=7.dp,bottom=4.dp))
   BasicTextField(value=value,onValueChange=change,singleLine=true,visualTransformation=if(secret)PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,textStyle=androidx.compose.ui.text.TextStyle(color=Color.White,fontSize=11.sp),modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(field).padding(11.dp),decorationBox={inner->Box{if(value.isEmpty())Text(if(secret)"API key" else label,color=muted,fontSize=11.sp);inner()}})
  }
  field("PROFILE NAME",name,{name=it.take(50)})
  field("BASE URL (HTTPS)",url,{url=it.take(500)})
  field("MODEL ID",model,{model=it.take(120)})
  field(if(editId!=null)"NEW API KEY (blank keeps saved key)" else "API KEY",key,{key=it.take(500)},true)
  Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   Text(if(editId==null)"ADD API PROFILE" else "SAVE CHANGES",color=Color.White,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(violet).clickable {
    val ok=GalaxyApiProfiles.save(context,editId,name,url,model,key)
    status=if(ok){refresh++;key="";editId=null;"Profile saved. API key encrypted on this device."}else "Save failed. Check HTTPS URL, name, model and API key."
   }.padding(12.dp))
   if(editId!=null) Text("CANCEL",color=cyan,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clip(RoundedCornerShape(9.dp)).background(field).clickable{reset()}.padding(12.dp))
  }
  Spacer(Modifier.height(15.dp))
  Text("SAVED PROVIDERS • "+profiles.size,color=cyan,fontSize=10.sp,fontWeight=FontWeight.Bold)
  if(profiles.isEmpty()) Text("এখনো কোনো profile নেই। উপরে OpenAI API key দিয়ে প্রথম profile যোগ করো।",color=muted,fontSize=10.sp,modifier=Modifier.padding(top=8.dp))
  profiles.forEachIndexed { index,p ->
   Column(Modifier.fillMaxWidth().padding(top=8.dp).clip(RoundedCornerShape(12.dp)).background(field).padding(10.dp)) {
    Row(verticalAlignment=Alignment.CenterVertically) {
     Column(Modifier.weight(1f)) {
      Text((if(index==0)"★ PRIMARY • " else "")+p.name,color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Bold)
      Text(p.model+" • "+(if(p.hasKey)"Key saved" else "Missing key"),color=muted,fontSize=9.sp)
     }
     Switch(checked=p.enabled,onCheckedChange={GalaxyApiProfiles.enable(context,p.id,it);refresh++})
    }
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.padding(top=5.dp)) {
     Text("MAKE PRIMARY",color=cyan,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clickable{GalaxyApiProfiles.primary(context,p.id);refresh++})
     Text("EDIT",color=Color.White,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clickable{editId=p.id;name=p.name;url=p.baseUrl;model=p.model;key=""})
     Text("DELETE",color=Color(0xFFFF9AAB),fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clickable{GalaxyApiProfiles.delete(context,p.id);refresh++;if(editId==p.id)reset()})
    }
   }
  }
  Text("TEST FAILOVER",color=cyan,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=14.dp).clip(RoundedCornerShape(9.dp)).background(field).clickable(enabled=!testing&&profiles.any{it.enabled&&it.hasKey}) {
   testing=true;status="Primary ও backup provider পরীক্ষা হচ্ছে…"
   GalaxyMultiApiClient.send(context,"Reply with exactly: Galaxy API connection verified."){result->testing=false;status=result.fold({"Connection successful. Active provider responded."},{"All tested profiles failed: "+(it.message?: "Unknown error")})}
  }.padding(horizontal=13.dp,vertical=10.dp))
  Text(status,color=muted,fontSize=9.sp,lineHeight=14.sp,modifier=Modifier.padding(top=8.dp))
  Text("নোট: Failover সব HTTP/network error-এ পরের profile চেষ্টা করে। API ব্যবহার করলে সংশ্লিষ্ট provider-এর billing প্রযোজ্য। APK-তে থাকা key rooted/compromised device থেকে পুরোপুরি সুরক্ষিত নয়; production-এর জন্য backend proxy উত্তম।",color=muted,fontSize=9.sp,lineHeight=14.sp,modifier=Modifier.padding(top=8.dp))
 }
}
