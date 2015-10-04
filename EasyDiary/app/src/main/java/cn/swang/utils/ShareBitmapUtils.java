package cn.swang.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;


import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import cn.swang.R;
import cn.swang.app.GlobalData;
import cn.swang.app.IConstants;
import cn.swang.entity.DayCard;
import cn.swang.entity.NoteCard;

/**
 * Created by sw on 2015/9/21.
 */
public class ShareBitmapUtils {

    public void convertDayCardBitmap(ConvertDayCardListener listener,DayCard dayCard){
        new ConvertDayCardAsyncTask(listener,dayCard).execute();
    }

    public interface ConvertDayCardListener{
        void onConvertSuccess(String imagePath);
    }

    private class ConvertDayCardAsyncTask extends AsyncTask<Void,Void,String>{
        private ConvertDayCardListener listener;
        private DayCard dayCard;
        public ConvertDayCardAsyncTask(ConvertDayCardListener listener,DayCard dayCard){
            this.listener=listener;
            this.dayCard=dayCard;
        }

        @Override
        protected String doInBackground(Void... params) {
            return convertDayCardToBitmap(dayCard);
        }

        @Override
        protected void onPostExecute(String bitmap) {
            super.onPostExecute(bitmap);
            if(listener!=null){
                listener.onConvertSuccess(bitmap);
            };
        }
    }

    public String convertDayCardToBitmap(DayCard dayCard){
        HashMap<String,Bitmap> hashMap=new HashMap<String,Bitmap>();
        int bitmap_height = 180;//header+footer
        int bitmap_width = 600;
        int textWordCount = 22;
        int textHeight = 30;
        int bitmapMargin = 30;
        float bitmapRadio = 0.83f;
        //caculate width height
        for(NoteCard noteCard:dayCard.getNoteSet()){
            if(!TextUtils.isEmpty(noteCard.getVoicePath())) continue;
            if(!TextUtils.isEmpty(noteCard.getContent())){
                bitmap_height+=Math.ceil(noteCard.getContent().length() * 1.0 / textWordCount)*textHeight;
            }else if(!TextUtils.isEmpty(noteCard.getImgPath())){
                final String key = noteCard.getImgPath();
                String uri = ImageDownloader.Scheme.FILE.wrap(noteCard.getImgPath());
                Bitmap mbitmap=BitmapFactory.decodeFile(noteCard.getImgPath());
                if(mbitmap.getWidth()>bitmap_width){
                    int height = (int) ((mbitmap.getHeight()*bitmap_width/mbitmap.getWidth())*bitmapRadio);
                    mbitmap = zoomImage(mbitmap,bitmap_width*bitmapRadio,height);
                }
                bitmap_height+=(mbitmap.getHeight()+bitmapMargin);
                hashMap.put(key,mbitmap);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitmap_width,bitmap_height, Bitmap.Config.ARGB_8888);
        Canvas canvas =new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DEV_KERN_TEXT_FLAG);
        paint.setTextSize(23.0f);
        canvas.drawColor(Color.WHITE);
        int x=50,y=30;

        //draw title
        paint.setColor(Color.GRAY);
        String title = dayCard.getYear()+"/"+dayCard.getMouth()+"/"+dayCard.getDay();
        canvas.drawText(title,bitmap_width-130,y,paint);
        y+=15;
        canvas.drawLine(10,y,bitmap_width-10,y+1,paint);
        paint.setColor(Color.BLACK);
        y+=50;

        //y=95
        for(NoteCard noteCard:dayCard.getNoteSet()){
            if(!TextUtils.isEmpty(noteCard.getVoicePath())) continue;
            if(!TextUtils.isEmpty(noteCard.getContent())){
                //draw text
                String text = noteCard.getContent();
                int k=0;
                while(k+textWordCount<=text.length()){
                    String tmp = text.substring(k, k+textWordCount);
                    canvas.drawText(tmp, x, y, paint);
                    y+=textHeight;
                    k=k+textWordCount;
                }
                String tmp2 = text.substring(k,text.length());
                canvas.drawText(tmp2,x,y,paint);
                y+=textHeight;
            }else if(!TextUtils.isEmpty(noteCard.getImgPath())){
                //draw bitmap
                canvas.drawBitmap(hashMap.get(noteCard.getImgPath()),x,y,paint);
                y+=(hashMap.get(noteCard.getImgPath()).getHeight()+bitmapMargin);
            }
        }
        //draw end sign
        y+=40;
        paint.setColor(Color.GRAY);
        canvas.drawLine(10,y,bitmap_width-10,y+1,paint);
        y+=30;
        String endSign = GlobalData.app().getString(R.string.app_name);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(endSign, 300, y, paint);

        canvas.save(canvas.ALL_SAVE_FLAG);//保存所有图层
        canvas.restore();
        File newFIle = new File(Environment.getExternalStorageDirectory()+ IConstants.SHARE_PHOTO_PATH+dayCard.getYear()+"_"+dayCard.getMouth()+"_"+dayCard.getDay()+".jpg");
        newFIle.getParentFile().mkdir();
        saveBitmaptoFile(bitmap,newFIle.getAbsolutePath());
        return newFIle.getAbsolutePath();
        //String imagePath = Environment.getExternalStorageDirectory()+ IConstants.SHARE_PHOTO_PATH+dayCard.getYear()+"_"+dayCard.getMouth()+"_"+dayCard.getDay()+".jpg";
        //saveBitmaptoFile(bitmap,imagePath);
        //return imagePath;
    }

    public Bitmap zoomImage(Bitmap bgimage, double newWidth,
                                   double newHeight) {
        // 获取这个图片的宽和高
        float width = bgimage.getWidth();
        float height = bgimage.getHeight();
        // 创建操作图片用的matrix对象
        Matrix matrix = new Matrix();
        // 计算宽高缩放率
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 缩放图片动作
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap bitmap = Bitmap.createBitmap(bgimage, 0, 0, (int) width,
                (int) height, matrix, true);
        return bitmap;
    }

    public void saveBitmaptoFile(Bitmap bmp,String imagePath){
        FileOutputStream fop;
        try {
            //实例化FileOutputStream，参数是生成路径
            fop=new FileOutputStream(imagePath);
            //压缩bitmap写进outputStream 参数：输出格式  输出质量  目标OutputStream
            //格式可以为jpg,png,jpg不能存储透明
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fop);
            fop.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}