package com.example.myapplication.api;

import com.example.myapplication.models.*;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Получить все метки в bounding box
    @GET("locations/")
    Call<List<LocationSeat>> getLocations(
            @Header("Authorization") String token,
            @Query("min_lat") Double minLat,
            @Query("max_lat") Double maxLat,
            @Query("min_lon") Double minLon,
            @Query("max_lon") Double maxLon,
            @Query("type_id") Integer typeId,
            @Query("status_id") Integer statusId
    );

    // Создать новую метку
    @POST("locations/")
    Call<LocationSeat> createLocation(
            @Header("Authorization") String token,
            @Body LocationSeatCreate locationData
    );

    // Удалить метку
    @DELETE("locations/{location_id}")
    Call<ResponseBody> deleteLocation(
            @Header("Authorization") String token,
            @Path("location_id") int locationId
    );

    // Получить мои метки
    @GET("locations/my")
    Call<List<LocationSeat>> getMyLocations(
            @Header("Authorization") String token
    );

    // Авторизация - используем LoginRequest из models
    @FormUrlEncoded
    @POST("/login")
    Call<AuthResponse> login(
            @Field("username") String username,
            @Field("password") String password
    );

    // Получить информацию о текущем пользователе
    @GET("/me")
    Call<User> getCurrentUser(
            @Header("Authorization") String token
    );

    // Регистрация
    @POST("/register")
    Call<AuthResponse> register(
            @Body RegisterRequest registerRequest
    );

    @POST("reviews/") // Ссылка теперь простая, без фигурных скобок
    Call<com.example.myapplication.models.Review> addReview(
            @Header("Authorization") String token,
            // @Path удаляем, так как ID теперь внутри @Body
            @Body ReviewCreate review
    );
    @Multipart
    @POST("pictures/upload") // Проверьте, совпадает ли с путем в Python (без префикса роутера или с ним)
    Call<Object> uploadPicture( // Можно создать модель PictureResponse, но пока Object сойдет
                                @Header("Authorization") String token,
                                @Query("location_id") int locationId,
                                @Part MultipartBody.Part file
    );

    @GET("locations/") // Путь к вашему эндпоинту locations_router
    Call<List<LocationSeat>> getLocations(
            @Query("min_lat") double minLat,
            @Query("max_lat") double maxLat,
            @Query("min_lon") double minLon,
            @Query("max_lon") double maxLon,
            @Query("type_id") Integer typeId,     // Можно null
            @Query("status_id") Integer statusId);  // Можно null
}


