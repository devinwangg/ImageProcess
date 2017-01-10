import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.Arrays;

public class ImageProcess {
    public static void main(String args[]){
        //讀取資料檔
        File file  = new File("image/original.jpg");
        try {
            BufferedImage RGB_img = ImageIO.read(file);
            //讀取高度
            int height = RGB_img.getHeight();
            //讀取寬度
            int width = RGB_img.getWidth();
            //灰階轉換
            int [] gray_img = new int[width * height];
            gray_img = Gray(RGB_img);
            //負片轉換
            Negative(gray_img, width, height);

            //Gamma 轉換
            int [] gamma_img1 = new int[width * height];
            int [] gamma_img2 = new int[width * height];
            int [] gamma_img3 = new int[width * height];
            gamma_img1 = Gamma(gray_img, width, height, 0.5);
            gamma_img2 = Gamma(gray_img, width, height, 1);
            gamma_img3 = Gamma(gray_img, width, height, 2);

            //胡椒鹽雜訊
            int [] peper_img = new int[width * height];
            peper_img = Pepper(gamma_img1, width, height);
            //中位數
            Median(peper_img, width, height);
            //平均濾波器
            Mean(peper_img, width, height);
            //Sobel
            Sobel(gamma_img2, width, height);
            //二值化
            OTSU(gamma_img3, width, height);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //灰階轉換
    public static int[] Gray(BufferedImage RGB_img){
        int width = RGB_img.getWidth();
        int height = RGB_img.getHeight();
        int [] pixels = new int[width * height];
        int [] pixels_result = new int[width * height];

        // Get RGB pixels
        RGB_img.getRGB(0, 0, width, height, pixels, 0, width);

        /*
          RGB Process
          int red      = (pixel >> 16) & 0xff; 右移16位之後原来的高8位就在低8位的位置上了，再與0xff就只剩下了原来的高8位數值
          int green    = (pixel >>  8) & 0xff; 右移8位之後原来的中間8位就在低8位的位置上了，再與0xff就只剩下了原来的中間8位數值
          int blue     = (pixel      ) & 0xff; 與0xff就直接得到了低8位数值
        */
        for(int i = 0; i < width*height ; i++){
            int rgb = pixels[i];
            int red = (rgb & 0xff0000) >> 16;
            int green= (rgb & 0x00ff00) >> 8;
            int blue= rgb & 0x0000ff;
            //轉換灰階公式
            int gray = (int)(0.299 * red + 0.587 * green + 0.114 * blue); // 由 RGB 來計算 Y 值
            pixels[i] = (0xff000000 | gray<<16 | gray<<8 | gray);
            pixels_result[i] = gray; //解決黃點問題
        }

        BufferedImage gray_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);  //image 轉 BfferedImage
        gray_image.setRGB(0, 0, width, height, pixels, 0, width);
        try {
            File file_gray = new File("gray.bmp");
            ImageIO.write(gray_image, "bmp", file_gray);
        }catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return pixels_result;
    }

    //負片 （255-原像素值）
    public static void Negative(int[] gray_img, int width, int height){
        int [] pixels = new int[gray_img.length];

        for(int i = 0; i < gray_img.length; i++){

            int negative = 255 - gray_img[i];
            pixels[i] = (0xff000000 | negative<<16 | negative<<8 | negative);
        }

        BufferedImage negative_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);  //image 轉 BfferedImage
        negative_image.setRGB(0, 0, width, height, pixels, 0, width);

        try {
            File file_negative = new File("negative.bmp");
            ImageIO.write(negative_image, "bmp", file_negative);
        }catch (Exception e) {}
    }

    public static int[] Gamma(int [] gray_img, int width, int height, double gamma_value){
        int max = -1;
        int min = 256;
        int [] pixels = new int[gray_img.length];
        int [] pixels_result = new int[gray_img.length];

        for(int i = 0; i < gray_img.length; i++){ //找max and min
            if(gray_img[i] > max){
                max = gray_img[i];
            }
            if(gray_img[i] < min){
                min = gray_img[i];
            }
        }


        /*
          Gamma公式
          [(p(i,j)-min/max-min)^gamma]*255
        	p(i,j)為像素點之值，min為圖片中像素之最小值，max為圖片中像素之最大值
        */
        for(int i = 0; i < gray_img.length; i++){
            double gamma_double = Math.pow((double)(gray_img[i] - min)/(max - min), gamma_value)*255;
            int gamma = (int) gamma_double;
            pixels[i] = (0xff000000 | gamma << 16 | gamma << 8 | gamma);
            pixels_result[i] = gamma;
        }

        BufferedImage gama_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); //image 轉 BfferedImage
        gama_image.setRGB(0, 0, width, height, pixels, 0, width);

        try {
            File file_gama = new File("gamma_" + gamma_value + ".bmp");
            ImageIO.write(gama_image, "bmp", file_gama);
        }catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return pixels_result;
    }


    //胡椒鹽雜訊
    public static int[] Pepper(int [] gamma_img, int width, int height){
        int [] pixels = new int[gamma_img.length];
        int [] pepper_result = new int[gamma_img.length];

        for(int i = 0; i < gamma_img.length; i++){
            pepper_result[i] = gamma_img[i];
        }

        Random random = new Random();
        for(int i = 0; i < gamma_img.length; i++){

            int pepper = random.nextInt(10);

            if(pepper == 9){
                pepper_result[i]=0;
            }
            else if(pepper == 0){
                pepper_result[i]=255;
            }
            pixels[i] = (0xff000000 | pepper_result[i] << 16 | pepper_result[i] << 8 | pepper_result[i]);
        }

        BufferedImage pepper_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); //image 轉 BfferedImage
        pepper_image.setRGB(0, 0, width, height, pixels, 0, width);

        try {
            File file_pepper = new File("peper.bmp");
            ImageIO.write(pepper_image, "bmp", file_pepper);
        }catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return pepper_result;
    }

    //中值濾波器（將九個值進行排序，利用中間的值取代最大的值（即九宮格右下的值））
    public static void Median(int [] peper_img, int width, int height){
        int [] pixels = new int[peper_img.length];
        int [] median_result = new int[peper_img.length];

        for(int i = 0; i < peper_img.length; i++){
            median_result[i] = peper_img[i];
        }

        for(int i = 1; i < width - 1; i++){
            for(int j = 1; j < height - 1 ; j++){
                int [] window = new int[9];

                window[0]=peper_img[width*j + i - width - 1]; //左上
                window[1]=peper_img[width*j + i - width]; //上
                window[2]=peper_img[width*j + i - width + 1]; //右上
                window[3]=peper_img[width*j + i - 1]; //左
                window[4]=peper_img[width*j + i]; //中
                window[5]=peper_img[width*j + i + 1]; //右
                window[6]=peper_img[width*j + i + width - 1]; //左下
                window[7]=peper_img[width*j + i + width]; //下
                window[8]=peper_img[width*j + i + width + 1]; //右下

                Arrays.sort(window);
                median_result[width*j + i] = window[4]; //取代
            }
        }

        for(int i = 0; i < peper_img.length; i++){
            pixels[i] = (0xff000000 | median_result[i] << 16 | median_result[i] << 8 | median_result[i]);
        }

        BufferedImage median_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); //image 轉 BfferedImage
        median_image.setRGB(0, 0, width, height, pixels, 0, width);

        try {
            File file_median = new File("median.bmp");
            ImageIO.write(median_image, "bmp", file_median);
        }catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //平均濾波器（將九宮格每一格數值相加取平均值，並將此數值取代全部值）
    public static void Mean(int [] peper_img, int width, int height){
        int mean[]=new int[peper_img.length];
        int mean_result[]= new int[peper_img.length];

        for(int i = 0; i < peper_img.length; i++){
            mean_result[i] = peper_img[i];
        }

        for(int i = 1; i < width - 1; i++){
            for(int j = 1; j < height - 1; j++){
                int window[]=new int[9];

                window[0]=peper_img[width*j + i - width - 1]; //左上
                window[1]=peper_img[width*j + i - width]; //上
                window[2]=peper_img[width*j + i - width + 1]; //右上
                window[3]=peper_img[width*j + i - 1]; //左
                window[4]=peper_img[width*j + i]; //中
                window[5]=peper_img[width*j + i + 1]; //右
                window[6]=peper_img[width*j + i + width - 1]; //左下
                window[7]=peper_img[width*j + i + width]; //下
                window[8]=peper_img[width*j + i + width + 1]; //右下

                int total = 0;
                for(int k = 0; k < 9; k++) {
                    total += window[k];
                }

                double average = total / 9;
                mean_result[width*j + i] = (int)average; //取代
            }
        }

        for(int i = 0; i < peper_img.length; i++){
            mean[i] = (0xff000000 | mean_result[i] << 16 | mean_result[i] << 8 | mean_result[i]);
        }

        BufferedImage mean_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);  //image 轉 BfferedImage
        mean_image.setRGB(0, 0, width, height, mean, 0, width);

        try {
            File file_mean = new File("mean.bmp");
            ImageIO.write(mean_image, "bmp", file_mean);
        }catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //二值化（設門檻值(所有像素之平均值)，大於門檻值則設255，小於則設0）
    public static void OTSU(int [] gamma_img, int width, int height){
        int threshold = 0;
        int total = 0;
        int [] pixels = new int[width * height];
        int [] otsu_result = new int[width * height];

        for (int i = 0; i < gamma_img.length ;i++){
            otsu_result[i] = gamma_img[i];
        }

        for(int i = 0; i < gamma_img.length ;i++){
            total += gamma_img[i];
        }

        threshold = total / (width*height);

        for(int i = 0; i < gamma_img.length; i++){
            if(gamma_img[i] >= threshold)
                otsu_result[i] = 255;
            else
                otsu_result[i] = 0;

            pixels[i] = (0xff000000 | otsu_result[i] << 16 | otsu_result[i] << 8 | otsu_result[i]);
        }

        BufferedImage otsu_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);  //image 轉 BfferedImage
        otsu_image.setRGB(0, 0, width, height, pixels, 0, width);

        try {
            File file_otsu = new File("otsu.bmp");
            ImageIO.write(otsu_image, "bmp", file_otsu);
        }catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //sobel轉換
    public static void Sobel(int [] gamma_img, int width, int height){
        int [] pixels = new int[gamma_img.length];
        int [][] Gx = new int[height][width];
        int [][] Gy = new int[height][width];
        int [][] G = new int[height][width];

        int [][] original = new int[height][width]; //gamma2
        int count = 0;
        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                original[i][j] = gamma_img[count];
                count ++;
            }
        }

        int count2= 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (i == 0 || i == height - 1 || j == 0 || j == width - 1)
                    Gx[i][j] = Gy[i][j] = G[i][j] = 0; // image boundary cleared
                else {
                    Gy[i][j] =
                        original[i+1][j-1] + 2*original[i+1][j] + original[i+1][j+1] -
                        original[i-1][j-1] - 2*original[i-1][j] - original[i-1][j+1];
                    Gx[i][j] =
                        original[i-1][j+1] + 2*original[i][j+1] + original[i+1][j+1] -
                        original[i-1][j-1] - 2*original[i][j-1] - original[i+1][j-1];
                    //公式（soble = sqrt[(x^2+y^2)]）
                    G[i][j] =(int)(Math.sqrt((Math.pow(Gx[i][j],2) + Math.pow(Gy[i][j],2))));
                }
                pixels[count2] = (0xff000000 | G[i][j] << 16 | G[i][j] << 8 | G[i][j]);
                count2 ++;
            }
        }

        BufferedImage sobel_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);  //image 轉 BfferedImage
        sobel_image.setRGB(0, 0, width, height, pixels, 0, width);

        try {
            File file_sobel = new File("sobel.bmp");
            ImageIO.write(sobel_image, "bmp", file_sobel);
        }catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
