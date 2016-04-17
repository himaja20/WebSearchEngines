package PageRank;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Parser {
  
  public boolean hasOutlinks(String pageFile) throws IOException{
    Document doc = Jsoup.parse(new File(pageFile),"UTF-8","");
    Elements links = doc.select("a[href]");
    if (links.size()!= 0){
      return true;
    }
    return false;
  }
  
  public Map<String,Integer> getOutlinks(String pageFile) throws IOException{
    if (!hasOutlinks(pageFile)){
      return null;
    }
    int weight = 1;
    Map<String,Integer> ret = new HashMap<String,Integer>();
    final String nodeFilter = "\\b(H1|H2|H3|H4|EM|B)\\b";
    Document doc = Jsoup.parse(new File(pageFile),"UTF-8","");
    Elements links = doc.select("a[href]");
    for (Element link : links){
      Elements parents = link.parents();
      for (Element parent : parents){
        String node = parent.nodeName();
        if (node.toUpperCase().matches(nodeFilter)){
          weight++;
          break;
        }
      }
      if (ret.containsKey(link.attr("href"))){
        weight += ret.get(link.attr("href"));
        ret.put(link.attr("href"), weight);
      }
      else{
        ret.put(link.attr("href"),weight);
      }
      weight = 1;
    }
    return ret;
  }
  
  public int getWordCount(String pageFile) throws IOException{
    Document doc = Jsoup.parse(new File(pageFile),"UTF-8","");
    String text = doc.text().trim();
    String[] tokens = text.split(" ");
    return tokens.length;
  }

}
