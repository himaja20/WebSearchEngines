package PageRank;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/*
 * TODO
 * Cleaning up of code
 */
public class PageRank {
  
  @Parameter(names={"-docs", "-d"},required = true)
  private String docs;
  @Parameter(names={"-F", "-f"},required = true)
  private float f;

  public static void main(String[] args) throws IOException {
    PageRank pr = new PageRank();
    new JCommander(pr, args);
    pr.rank();
  }

  public void rank() throws IOException{

    Parser parser = new Parser();
    ArrayList<Page> pages = new ArrayList<Page>();
    Map<String, Integer> outlinks = new HashMap<String,Integer>();
    Map<String,Page> linkToPage = new HashMap<String,Page>();

    File folder = new File(docs);
    File[] listOfFiles = folder.listFiles();
    Weights weightObj = new Weights(listOfFiles.length);

    for (File file : listOfFiles) {
      if (file.isFile()) {
        int wordCount = parser.getWordCount(file.getAbsolutePath());
        Page page = new Page(wordCount,file.getAbsolutePath());
        pages.add(page);
        linkToPage.put(file.getName(), page);
      }
    }
    float baseSum = 0;
    for (int i = 0; i <pages.size();i++){
      baseSum += pages.get(i).getBase();
    }
    for (Page page: pages){
      page.setBase(page.getBase()/baseSum);
      page.setScore(page.getBase());
    }

    for (int i = 0; i <pages.size();i++){
      Page p = pages.get(i);
      if (!parser.hasOutlinks(p.getFileLocation())){
        for(Page q: pages){
          weightObj.setWeights(q,p,q.getScore());
        }
      }
      else{
        outlinks = parser.getOutlinks(p.getFileLocation());
        for (Entry<String, Integer> entry : outlinks.entrySet())
        {
          Page q = linkToPage.get(entry.getKey());
          float weight = entry.getValue();
          weightObj.setWeights(q, p, weight);
        }

        int sum = 0;
        for (Entry<String, Integer> entry : outlinks.entrySet())
        {
          Page q = linkToPage.get(entry.getKey());
          sum += weightObj.getWeights(q, p);
        }

        for (Entry<String, Integer> entry : outlinks.entrySet())
        {
          Page q = linkToPage.get(entry.getKey());
          weightObj.setWeights(q, p, weightObj.getWeights(q, p)/sum);
        }
      }
    }
    boolean changed = true;
    float epsilon = (float) (0.01/pages.size());
    while(changed){
      changed = false;
      
      for (Page pPage: pages){
        float normalizedScore = 0;
        for (Page qPage:pages){
          normalizedScore += (qPage.getScore() * weightObj.getWeights(pPage, qPage));
        }
        float newScore = ((1-f) * pPage.getBase()) + (f * normalizedScore);
        pPage.setNewScore(newScore);
        
        if (Math.abs(pPage.getNewScore()- pPage.getScore()) > epsilon){
          changed = true;
        }
      }
      for (Page pPage: pages){
        pPage.setScore(pPage.getNewScore());
      }
    }
    Collections.sort(pages,new PageComparator());
    for (Page pPage:pages){
      String filename = pPage.getFileName();
      filename = filename.replaceFirst("[.][^.]+$", "");
      System.out.println(filename + " " + pPage.getScore());
    }
  }
}
