package PageRank;

import java.nio.file.Paths;

public class Page {
  private float base;
  private float score;
  private String fileLocation;
  private int id;
  private float newScore;
  private String fileName;
  private static int counter = 0;
  
  public Page(int wordCount, String fileLoc){
    base = log(wordCount, 2);
    score = 0;
    newScore = 0;
    id = counter++;
    fileLocation = fileLoc; 
    fileName = Paths.get(fileLocation).getFileName().toString();
  }
  
  public int getId(){
    return this.id;
  }
  
  public float getNewScore(){
    return this.newScore;
  }
  
  public void setNewScore(float newScore){
    this.newScore = newScore;
  }
  
  public String getFileName(){
    return this.fileName;
  }
  
  public String getFileLocation(){
    return this.fileLocation;
  }
  public float getBase(){
    return this.base;
  }
 
  public float getScore(){
    return this.score;
  }
  
  public void setBase(float base){
    this.base = base;
  }
  
  public void setScore(float score){
    this.score = score;
  }
  
  private static float log(int x, int base)
  {
    return (float) (Math.log(x) / Math.log(base));
  }
}


