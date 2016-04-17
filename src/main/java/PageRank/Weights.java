package PageRank;

public class Weights {
  private float[][] weights;

  public Weights(int numPages){
    weights = new float[numPages][numPages];
  }

  public void setWeights(Page q, Page p, float f){
    weights[q.getId()][p.getId()] = f;
  }
  
  public float getWeights(Page q, Page p){
    return this.weights[q.getId()][p.getId()];
  }
  
//  public void printWeightMatrix(ArrayList<Page> pages){
//    for (Page page: pages){
//      System.out.print("                  " + page.getFileName() + " ");
//    }
//    System.out.println();
//    for (int i = 0; i<weights.length ; i++){
//      System.out.print(pages.get(i).getFileName() + "                ");
//      for(int j = 0; j<weights.length; j++){
//        System.out.print(weights[i][j] + "           ");
//      }
//      System.out.println();
//    }
//  }
  
}
