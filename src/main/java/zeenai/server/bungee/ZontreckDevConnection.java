package zeenai.server.bungee;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class ZontreckDevConnection
{
    public class Reply{
        public int Status;
        public String body;

        public Reply(int stat, String cont){
            Status=stat;
            body=cont;
        }
    }

    public Reply SendRequest(boolean Post, boolean EAQMS, String Script, String PostBody){
        String url = ZNIPlugin.getInstance().CFG.getString("eaqms.url");
        if(EAQMS) url += "EAQMS/";
        url+=Script;

        URL _url = null;
        try{
            _url = new URL(url);
            
            String method = "GET";
            if(Post)method="POST";
            HttpsURLConnection con = (HttpsURLConnection)_url.openConnection();
            con.setRequestMethod(method);
            con.setDoOutput(true);
            con.setDoInput(true);
            DataOutputStream dos = new DataOutputStream(con.getOutputStream());
            dos.writeBytes(PostBody);

            dos.flush();
            dos.close();
            String response = "";
            String input="";
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            while((input = br.readLine())!=null){
                response+=input;
            }
            br.close();

            Reply rep = new Reply(con.getResponseCode(), response);

            return rep;
        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
        
    }
}