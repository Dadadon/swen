package com.example.recipeapp;

import android.app.ProgressDialog;
import android.net.Uri;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.google.firebase.storage.StorageReference;

public class AddRecipe extends AppCompatActivity {
    Uri uriVideo, image;
    byte[] bytes;
    String recipeName, videoUrl, recipeImageurl;
    String videoLength;
    private StorageReference storageReference;
    ProgressDialog progressDialog;
    EditText selectRecipeName;
    EditText recipe;
    ImageView selectRecipeImage;
    Button uploadRecipeButton;
    ImageButton selectVideo;
    private int rCount=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Add Recipe");
        storageReference = FirebaseStorage.getInstance().getReference();
        progressDialog = new ProgressDialog(this);

        selectRecipeName = findViewById(R.id.selectRecipeVid);
        selectRecipeImage = findViewById(R.id.selectRecipeImage);
        uploadRecipeButton = findViewById(R.id.addRecipe);
        recipe = findViewById(R.id.recipe);
        selectVideo = findViewById(R.id.selectVideoButton);

        selectVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickVideo();
            }
        });

        selectRecipeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickImage();
            }
        });
    }

    // SELECT THE VIDEO TO UPLOAD FROM MOBILE STORAGE
    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent,1);
    }


    //SELECT THE IMAGE TO UPLOAD FROM MOBILE STORAGE
    private void  pickImage(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent,2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            if (requestCode == 1 && resultCode == RESULT_OK) {
                uriVideo = data.getData();
//                Log.i("uri", songName.toString());
                RecipeName = getFileName(uriVideo);
                selectRecipeName.setText(RecipeName);
                videoLength = getVideoDuration(uriVideo);
                Log.i("duration", videoLength);
            }
            if (requestCode == 2 && resultCode == RESULT_OK){
//                Log.i("image",data.toString());
                image = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),image);
                    selectRecipeImage.setImageBitmap(bitmap);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
                    bytes = byteArrayOutputStream.toByteArray();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void addRecipe(View view){
        if (uriVideo == null){
            Toast.makeText(this, "Please select a video", Toast.LENGTH_SHORT).show();
        }
        else if (selectRecipeName.getText().toString().equals("")){
            Toast.makeText(this, "Recipe name cannot be empty!", Toast.LENGTH_SHORT).show();
        }
        else if(recipe.getText().toString().equals("")){
            Toast.makeText(this, "Please add Recipe", Toast.LENGTH_SHORT).show();
        }
        else if (image == null){
            Toast.makeText(this, "Please select a Recipe Thumbnail", Toast.LENGTH_SHORT).show();
        }
        else {
            RecipeName = selectRecipeName.getText().toString();
            String recipe2 = recipe.getText().toString();
            uploadRecipeImageToServer(bytes,prodName);
            uploadRecipeFileToServer(uriVideo,prodName,price,videoLength);
        }
    }

    public void uploadRecipeImageToServer(byte[] image, String fileName) {
        UploadTask uploadTask = storageRef.child("Thumbnails").child(fileName).putBytes(image);
        progressDialog.show();
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> task = taskSnapshot.getStorage().getDownloadUrl();
                while (!task.isComplete());
                Uri urlPic = task.getResult();
                RecipeImageUrl = urlPic.toString();
//                Log.i("image url", imageUrl);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("image url", "failed");
            }
        });
    }

    public void uploadRecipeFileToServer(Uri uri, final String recipeName, final String recipe, final String duration){
        StorageReference filePath = storageRef.child("Recipes").child(recipeName);
        FirebaseDatabase.getInstance().getReference().child("Recipes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    rCount= (int) snapshot.getChildrenCount();
                }
                else {
                    rCount=1;
                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

        progressDialog.show();
        filePath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                Log.i("success", "upload");
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isComplete());
                Uri urlVid = uriTask.getResult();
                videoUrl = urlVid.toString();

//                Log.i("success url ", songUrl);
                uploadProdDetailsToDatabase(recipeName,recipe,recipeImageUrl,videoUrl,duration);
//                progressDialog.dismiss();
            }

        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0*taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
                int currentProgress = (int) progress;
                progressDialog.setMessage("Uploading: " + currentProgress + "%");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //Log.i("success", "upload");
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "Upload Failed! Please Try again!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // UPLOAD RECIPE AND URLS TO REALTIME DATABASE
    public void uploadProdDetailsToDatabase(String recipeName, String recipe, String imageUrl, String videoUrl, String vidDuration){

        Recipes recipes = new Recipes(recipeName,videoUrl,imageUrl,recipe,Integer.toString(rCount));
        FirebaseDatabase.getInstance().getReference("Recipes")
                .push().setValue(product).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.i("database", "upload success");
                progressDialog2.dismiss();
                Toast.makeText(getApplicationContext(), "Recipe Uploaded to Database", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    public String getFileName(Uri uri) {
        String result = null;
        if (Objects.equals(uri.getScheme(), "content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            assert result != null;
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public String getVideoDuration(Uri video){
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(getApplicationContext(),video);
        String durationString = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long time = Long.parseLong(durationString);
        int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(time);
        int totalSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(time);
        int seconds = totalSeconds-(minutes*60);
        if (String.valueOf(seconds).length() == 1){
            return minutes + ":0" + seconds;
        }else {
            return minutes + ":" + seconds;
        }
    }
}
