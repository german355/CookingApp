package com.example.cooking.data.database;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Step;
import com.example.cooking.data.database.converters.DataConverters;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class RecipeDao_Impl implements RecipeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<RecipeEntity> __insertionAdapterOfRecipeEntity;

  private final EntityDeletionOrUpdateAdapter<RecipeEntity> __deletionAdapterOfRecipeEntity;

  private final EntityDeletionOrUpdateAdapter<RecipeEntity> __updateAdapterOfRecipeEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLikeStatus;

  private final SharedSQLiteStatement __preparedStmtOfClearAllLikeStatus;

  public RecipeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRecipeEntity = new EntityInsertionAdapter<RecipeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `recipes` (`id`,`title`,`ingredients`,`instructions`,`created_at`,`userId`,`mealType`,`foodType`,`photo_url`,`isLiked`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final RecipeEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getTitle() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getTitle());
        }
        final String _tmp = DataConverters.fromIngredientList(entity.getIngredients());
        if (_tmp == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, _tmp);
        }
        final String _tmp_1 = DataConverters.fromStepList(entity.getInstructions());
        if (_tmp_1 == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, _tmp_1);
        }
        if (entity.getCreated_at() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getCreated_at());
        }
        if (entity.getUserId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getUserId());
        }
        if (entity.getMealType() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getMealType());
        }
        if (entity.getFoodType() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getFoodType());
        }
        if (entity.getPhoto_url() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getPhoto_url());
        }
        final int _tmp_2 = entity.isLiked() ? 1 : 0;
        statement.bindLong(10, _tmp_2);
      }
    };
    this.__deletionAdapterOfRecipeEntity = new EntityDeletionOrUpdateAdapter<RecipeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `recipes` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final RecipeEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfRecipeEntity = new EntityDeletionOrUpdateAdapter<RecipeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `recipes` SET `id` = ?,`title` = ?,`ingredients` = ?,`instructions` = ?,`created_at` = ?,`userId` = ?,`mealType` = ?,`foodType` = ?,`photo_url` = ?,`isLiked` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final RecipeEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getTitle() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getTitle());
        }
        final String _tmp = DataConverters.fromIngredientList(entity.getIngredients());
        if (_tmp == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, _tmp);
        }
        final String _tmp_1 = DataConverters.fromStepList(entity.getInstructions());
        if (_tmp_1 == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, _tmp_1);
        }
        if (entity.getCreated_at() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getCreated_at());
        }
        if (entity.getUserId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getUserId());
        }
        if (entity.getMealType() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getMealType());
        }
        if (entity.getFoodType() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getFoodType());
        }
        if (entity.getPhoto_url() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getPhoto_url());
        }
        final int _tmp_2 = entity.isLiked() ? 1 : 0;
        statement.bindLong(10, _tmp_2);
        statement.bindLong(11, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM recipes";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM recipes WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateLikeStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE recipes SET isLiked = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAllLikeStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE recipes SET isLiked = 0";
        return _query;
      }
    };
  }

  @Override
  public void insertAll(final List<RecipeEntity> recipes) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfRecipeEntity.insert(recipes);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insert(final RecipeEntity recipe) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfRecipeEntity.insert(recipe);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final RecipeEntity recipe) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfRecipeEntity.handle(recipe);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final RecipeEntity recipe) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfRecipeEntity.handle(recipe);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void replaceAllRecipes(final List<RecipeEntity> recipes) {
    __db.beginTransaction();
    try {
      RecipeDao.super.replaceAllRecipes(recipes);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteAll() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteAll.release(_stmt);
    }
  }

  @Override
  public void deleteById(final int recipeId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, recipeId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteById.release(_stmt);
    }
  }

  @Override
  public void updateLikeStatus(final int recipeId, final boolean isLiked) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLikeStatus.acquire();
    int _argIndex = 1;
    final int _tmp = isLiked ? 1 : 0;
    _stmt.bindLong(_argIndex, _tmp);
    _argIndex = 2;
    _stmt.bindLong(_argIndex, recipeId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfUpdateLikeStatus.release(_stmt);
    }
  }

  @Override
  public void clearAllLikeStatus() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllLikeStatus.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfClearAllLikeStatus.release(_stmt);
    }
  }

  @Override
  public LiveData<List<RecipeEntity>> getAllRecipes() {
    final String _sql = "SELECT * FROM recipes ORDER BY title ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"recipes"}, false, new Callable<List<RecipeEntity>>() {
      @Override
      @Nullable
      public List<RecipeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
          final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
          final int _cursorIndexOfFoodType = CursorUtil.getColumnIndexOrThrow(_cursor, "foodType");
          final int _cursorIndexOfPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_url");
          final int _cursorIndexOfIsLiked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLiked");
          final List<RecipeEntity> _result = new ArrayList<RecipeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RecipeEntity _item;
            _item = new RecipeEntity();
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            _item.setId(_tmpId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            _item.setTitle(_tmpTitle);
            final List<Ingredient> _tmpIngredients;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfIngredients)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfIngredients);
            }
            _tmpIngredients = DataConverters.toIngredientList(_tmp);
            _item.setIngredients(_tmpIngredients);
            final List<Step> _tmpInstructions;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfInstructions)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfInstructions);
            }
            _tmpInstructions = DataConverters.toStepList(_tmp_1);
            _item.setInstructions(_tmpInstructions);
            final String _tmpCreated_at;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreated_at = null;
            } else {
              _tmpCreated_at = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            _item.setCreated_at(_tmpCreated_at);
            final String _tmpUserId;
            if (_cursor.isNull(_cursorIndexOfUserId)) {
              _tmpUserId = null;
            } else {
              _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            }
            _item.setUserId(_tmpUserId);
            final String _tmpMealType;
            if (_cursor.isNull(_cursorIndexOfMealType)) {
              _tmpMealType = null;
            } else {
              _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
            }
            _item.setMealType(_tmpMealType);
            final String _tmpFoodType;
            if (_cursor.isNull(_cursorIndexOfFoodType)) {
              _tmpFoodType = null;
            } else {
              _tmpFoodType = _cursor.getString(_cursorIndexOfFoodType);
            }
            _item.setFoodType(_tmpFoodType);
            final String _tmpPhoto_url;
            if (_cursor.isNull(_cursorIndexOfPhotoUrl)) {
              _tmpPhoto_url = null;
            } else {
              _tmpPhoto_url = _cursor.getString(_cursorIndexOfPhotoUrl);
            }
            _item.setPhoto_url(_tmpPhoto_url);
            final boolean _tmpIsLiked;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsLiked);
            _tmpIsLiked = _tmp_2 != 0;
            _item.setLiked(_tmpIsLiked);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public List<RecipeEntity> getAllRecipesList() {
    final String _sql = "SELECT * FROM recipes ORDER BY title ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
      final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
      final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
      final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
      final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
      final int _cursorIndexOfFoodType = CursorUtil.getColumnIndexOrThrow(_cursor, "foodType");
      final int _cursorIndexOfPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_url");
      final int _cursorIndexOfIsLiked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLiked");
      final List<RecipeEntity> _result = new ArrayList<RecipeEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final RecipeEntity _item;
        _item = new RecipeEntity();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final String _tmpTitle;
        if (_cursor.isNull(_cursorIndexOfTitle)) {
          _tmpTitle = null;
        } else {
          _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
        }
        _item.setTitle(_tmpTitle);
        final List<Ingredient> _tmpIngredients;
        final String _tmp;
        if (_cursor.isNull(_cursorIndexOfIngredients)) {
          _tmp = null;
        } else {
          _tmp = _cursor.getString(_cursorIndexOfIngredients);
        }
        _tmpIngredients = DataConverters.toIngredientList(_tmp);
        _item.setIngredients(_tmpIngredients);
        final List<Step> _tmpInstructions;
        final String _tmp_1;
        if (_cursor.isNull(_cursorIndexOfInstructions)) {
          _tmp_1 = null;
        } else {
          _tmp_1 = _cursor.getString(_cursorIndexOfInstructions);
        }
        _tmpInstructions = DataConverters.toStepList(_tmp_1);
        _item.setInstructions(_tmpInstructions);
        final String _tmpCreated_at;
        if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
          _tmpCreated_at = null;
        } else {
          _tmpCreated_at = _cursor.getString(_cursorIndexOfCreatedAt);
        }
        _item.setCreated_at(_tmpCreated_at);
        final String _tmpUserId;
        if (_cursor.isNull(_cursorIndexOfUserId)) {
          _tmpUserId = null;
        } else {
          _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
        }
        _item.setUserId(_tmpUserId);
        final String _tmpMealType;
        if (_cursor.isNull(_cursorIndexOfMealType)) {
          _tmpMealType = null;
        } else {
          _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
        }
        _item.setMealType(_tmpMealType);
        final String _tmpFoodType;
        if (_cursor.isNull(_cursorIndexOfFoodType)) {
          _tmpFoodType = null;
        } else {
          _tmpFoodType = _cursor.getString(_cursorIndexOfFoodType);
        }
        _item.setFoodType(_tmpFoodType);
        final String _tmpPhoto_url;
        if (_cursor.isNull(_cursorIndexOfPhotoUrl)) {
          _tmpPhoto_url = null;
        } else {
          _tmpPhoto_url = _cursor.getString(_cursorIndexOfPhotoUrl);
        }
        _item.setPhoto_url(_tmpPhoto_url);
        final boolean _tmpIsLiked;
        final int _tmp_2;
        _tmp_2 = _cursor.getInt(_cursorIndexOfIsLiked);
        _tmpIsLiked = _tmp_2 != 0;
        _item.setLiked(_tmpIsLiked);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public RecipeEntity getRecipeById(final int recipeId) {
    final String _sql = "SELECT * FROM recipes WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, recipeId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
      final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
      final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
      final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
      final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
      final int _cursorIndexOfFoodType = CursorUtil.getColumnIndexOrThrow(_cursor, "foodType");
      final int _cursorIndexOfPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_url");
      final int _cursorIndexOfIsLiked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLiked");
      final RecipeEntity _result;
      if (_cursor.moveToFirst()) {
        _result = new RecipeEntity();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _result.setId(_tmpId);
        final String _tmpTitle;
        if (_cursor.isNull(_cursorIndexOfTitle)) {
          _tmpTitle = null;
        } else {
          _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
        }
        _result.setTitle(_tmpTitle);
        final List<Ingredient> _tmpIngredients;
        final String _tmp;
        if (_cursor.isNull(_cursorIndexOfIngredients)) {
          _tmp = null;
        } else {
          _tmp = _cursor.getString(_cursorIndexOfIngredients);
        }
        _tmpIngredients = DataConverters.toIngredientList(_tmp);
        _result.setIngredients(_tmpIngredients);
        final List<Step> _tmpInstructions;
        final String _tmp_1;
        if (_cursor.isNull(_cursorIndexOfInstructions)) {
          _tmp_1 = null;
        } else {
          _tmp_1 = _cursor.getString(_cursorIndexOfInstructions);
        }
        _tmpInstructions = DataConverters.toStepList(_tmp_1);
        _result.setInstructions(_tmpInstructions);
        final String _tmpCreated_at;
        if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
          _tmpCreated_at = null;
        } else {
          _tmpCreated_at = _cursor.getString(_cursorIndexOfCreatedAt);
        }
        _result.setCreated_at(_tmpCreated_at);
        final String _tmpUserId;
        if (_cursor.isNull(_cursorIndexOfUserId)) {
          _tmpUserId = null;
        } else {
          _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
        }
        _result.setUserId(_tmpUserId);
        final String _tmpMealType;
        if (_cursor.isNull(_cursorIndexOfMealType)) {
          _tmpMealType = null;
        } else {
          _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
        }
        _result.setMealType(_tmpMealType);
        final String _tmpFoodType;
        if (_cursor.isNull(_cursorIndexOfFoodType)) {
          _tmpFoodType = null;
        } else {
          _tmpFoodType = _cursor.getString(_cursorIndexOfFoodType);
        }
        _result.setFoodType(_tmpFoodType);
        final String _tmpPhoto_url;
        if (_cursor.isNull(_cursorIndexOfPhotoUrl)) {
          _tmpPhoto_url = null;
        } else {
          _tmpPhoto_url = _cursor.getString(_cursorIndexOfPhotoUrl);
        }
        _result.setPhoto_url(_tmpPhoto_url);
        final boolean _tmpIsLiked;
        final int _tmp_2;
        _tmp_2 = _cursor.getInt(_cursorIndexOfIsLiked);
        _tmpIsLiked = _tmp_2 != 0;
        _result.setLiked(_tmpIsLiked);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public RecipeEntity getRecipeByIdSync(final int recipeId) {
    final String _sql = "SELECT * FROM recipes WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, recipeId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
      final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
      final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
      final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
      final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
      final int _cursorIndexOfFoodType = CursorUtil.getColumnIndexOrThrow(_cursor, "foodType");
      final int _cursorIndexOfPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_url");
      final int _cursorIndexOfIsLiked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLiked");
      final RecipeEntity _result;
      if (_cursor.moveToFirst()) {
        _result = new RecipeEntity();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _result.setId(_tmpId);
        final String _tmpTitle;
        if (_cursor.isNull(_cursorIndexOfTitle)) {
          _tmpTitle = null;
        } else {
          _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
        }
        _result.setTitle(_tmpTitle);
        final List<Ingredient> _tmpIngredients;
        final String _tmp;
        if (_cursor.isNull(_cursorIndexOfIngredients)) {
          _tmp = null;
        } else {
          _tmp = _cursor.getString(_cursorIndexOfIngredients);
        }
        _tmpIngredients = DataConverters.toIngredientList(_tmp);
        _result.setIngredients(_tmpIngredients);
        final List<Step> _tmpInstructions;
        final String _tmp_1;
        if (_cursor.isNull(_cursorIndexOfInstructions)) {
          _tmp_1 = null;
        } else {
          _tmp_1 = _cursor.getString(_cursorIndexOfInstructions);
        }
        _tmpInstructions = DataConverters.toStepList(_tmp_1);
        _result.setInstructions(_tmpInstructions);
        final String _tmpCreated_at;
        if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
          _tmpCreated_at = null;
        } else {
          _tmpCreated_at = _cursor.getString(_cursorIndexOfCreatedAt);
        }
        _result.setCreated_at(_tmpCreated_at);
        final String _tmpUserId;
        if (_cursor.isNull(_cursorIndexOfUserId)) {
          _tmpUserId = null;
        } else {
          _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
        }
        _result.setUserId(_tmpUserId);
        final String _tmpMealType;
        if (_cursor.isNull(_cursorIndexOfMealType)) {
          _tmpMealType = null;
        } else {
          _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
        }
        _result.setMealType(_tmpMealType);
        final String _tmpFoodType;
        if (_cursor.isNull(_cursorIndexOfFoodType)) {
          _tmpFoodType = null;
        } else {
          _tmpFoodType = _cursor.getString(_cursorIndexOfFoodType);
        }
        _result.setFoodType(_tmpFoodType);
        final String _tmpPhoto_url;
        if (_cursor.isNull(_cursorIndexOfPhotoUrl)) {
          _tmpPhoto_url = null;
        } else {
          _tmpPhoto_url = _cursor.getString(_cursorIndexOfPhotoUrl);
        }
        _result.setPhoto_url(_tmpPhoto_url);
        final boolean _tmpIsLiked;
        final int _tmp_2;
        _tmp_2 = _cursor.getInt(_cursorIndexOfIsLiked);
        _tmpIsLiked = _tmp_2 != 0;
        _result.setLiked(_tmpIsLiked);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LiveData<List<RecipeEntity>> getLikedRecipes() {
    final String _sql = "SELECT * FROM recipes WHERE isLiked = 1 ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"recipes"}, false, new Callable<List<RecipeEntity>>() {
      @Override
      @Nullable
      public List<RecipeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
          final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
          final int _cursorIndexOfFoodType = CursorUtil.getColumnIndexOrThrow(_cursor, "foodType");
          final int _cursorIndexOfPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_url");
          final int _cursorIndexOfIsLiked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLiked");
          final List<RecipeEntity> _result = new ArrayList<RecipeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RecipeEntity _item;
            _item = new RecipeEntity();
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            _item.setId(_tmpId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            _item.setTitle(_tmpTitle);
            final List<Ingredient> _tmpIngredients;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfIngredients)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfIngredients);
            }
            _tmpIngredients = DataConverters.toIngredientList(_tmp);
            _item.setIngredients(_tmpIngredients);
            final List<Step> _tmpInstructions;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfInstructions)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfInstructions);
            }
            _tmpInstructions = DataConverters.toStepList(_tmp_1);
            _item.setInstructions(_tmpInstructions);
            final String _tmpCreated_at;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreated_at = null;
            } else {
              _tmpCreated_at = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            _item.setCreated_at(_tmpCreated_at);
            final String _tmpUserId;
            if (_cursor.isNull(_cursorIndexOfUserId)) {
              _tmpUserId = null;
            } else {
              _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            }
            _item.setUserId(_tmpUserId);
            final String _tmpMealType;
            if (_cursor.isNull(_cursorIndexOfMealType)) {
              _tmpMealType = null;
            } else {
              _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
            }
            _item.setMealType(_tmpMealType);
            final String _tmpFoodType;
            if (_cursor.isNull(_cursorIndexOfFoodType)) {
              _tmpFoodType = null;
            } else {
              _tmpFoodType = _cursor.getString(_cursorIndexOfFoodType);
            }
            _item.setFoodType(_tmpFoodType);
            final String _tmpPhoto_url;
            if (_cursor.isNull(_cursorIndexOfPhotoUrl)) {
              _tmpPhoto_url = null;
            } else {
              _tmpPhoto_url = _cursor.getString(_cursorIndexOfPhotoUrl);
            }
            _item.setPhoto_url(_tmpPhoto_url);
            final boolean _tmpIsLiked;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsLiked);
            _tmpIsLiked = _tmp_2 != 0;
            _item.setLiked(_tmpIsLiked);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public List<RecipeEntity> searchRecipesByTitle(final String query) {
    final String _sql = "SELECT * FROM recipes WHERE title LIKE '%' || ? || '%' ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (query == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, query);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
      final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
      final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
      final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
      final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
      final int _cursorIndexOfFoodType = CursorUtil.getColumnIndexOrThrow(_cursor, "foodType");
      final int _cursorIndexOfPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_url");
      final int _cursorIndexOfIsLiked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLiked");
      final List<RecipeEntity> _result = new ArrayList<RecipeEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final RecipeEntity _item;
        _item = new RecipeEntity();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final String _tmpTitle;
        if (_cursor.isNull(_cursorIndexOfTitle)) {
          _tmpTitle = null;
        } else {
          _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
        }
        _item.setTitle(_tmpTitle);
        final List<Ingredient> _tmpIngredients;
        final String _tmp;
        if (_cursor.isNull(_cursorIndexOfIngredients)) {
          _tmp = null;
        } else {
          _tmp = _cursor.getString(_cursorIndexOfIngredients);
        }
        _tmpIngredients = DataConverters.toIngredientList(_tmp);
        _item.setIngredients(_tmpIngredients);
        final List<Step> _tmpInstructions;
        final String _tmp_1;
        if (_cursor.isNull(_cursorIndexOfInstructions)) {
          _tmp_1 = null;
        } else {
          _tmp_1 = _cursor.getString(_cursorIndexOfInstructions);
        }
        _tmpInstructions = DataConverters.toStepList(_tmp_1);
        _item.setInstructions(_tmpInstructions);
        final String _tmpCreated_at;
        if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
          _tmpCreated_at = null;
        } else {
          _tmpCreated_at = _cursor.getString(_cursorIndexOfCreatedAt);
        }
        _item.setCreated_at(_tmpCreated_at);
        final String _tmpUserId;
        if (_cursor.isNull(_cursorIndexOfUserId)) {
          _tmpUserId = null;
        } else {
          _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
        }
        _item.setUserId(_tmpUserId);
        final String _tmpMealType;
        if (_cursor.isNull(_cursorIndexOfMealType)) {
          _tmpMealType = null;
        } else {
          _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
        }
        _item.setMealType(_tmpMealType);
        final String _tmpFoodType;
        if (_cursor.isNull(_cursorIndexOfFoodType)) {
          _tmpFoodType = null;
        } else {
          _tmpFoodType = _cursor.getString(_cursorIndexOfFoodType);
        }
        _item.setFoodType(_tmpFoodType);
        final String _tmpPhoto_url;
        if (_cursor.isNull(_cursorIndexOfPhotoUrl)) {
          _tmpPhoto_url = null;
        } else {
          _tmpPhoto_url = _cursor.getString(_cursorIndexOfPhotoUrl);
        }
        _item.setPhoto_url(_tmpPhoto_url);
        final boolean _tmpIsLiked;
        final int _tmp_2;
        _tmp_2 = _cursor.getInt(_cursorIndexOfIsLiked);
        _tmpIsLiked = _tmp_2 != 0;
        _item.setLiked(_tmpIsLiked);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LiveData<RecipeEntity> getRecipeEntityByIdLiveData(final int recipeId) {
    final String _sql = "SELECT * FROM recipes WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, recipeId);
    return __db.getInvalidationTracker().createLiveData(new String[] {"recipes"}, false, new Callable<RecipeEntity>() {
      @Override
      @Nullable
      public RecipeEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
          final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
          final int _cursorIndexOfFoodType = CursorUtil.getColumnIndexOrThrow(_cursor, "foodType");
          final int _cursorIndexOfPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_url");
          final int _cursorIndexOfIsLiked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLiked");
          final RecipeEntity _result;
          if (_cursor.moveToFirst()) {
            _result = new RecipeEntity();
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            _result.setId(_tmpId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            _result.setTitle(_tmpTitle);
            final List<Ingredient> _tmpIngredients;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfIngredients)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfIngredients);
            }
            _tmpIngredients = DataConverters.toIngredientList(_tmp);
            _result.setIngredients(_tmpIngredients);
            final List<Step> _tmpInstructions;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfInstructions)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfInstructions);
            }
            _tmpInstructions = DataConverters.toStepList(_tmp_1);
            _result.setInstructions(_tmpInstructions);
            final String _tmpCreated_at;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreated_at = null;
            } else {
              _tmpCreated_at = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            _result.setCreated_at(_tmpCreated_at);
            final String _tmpUserId;
            if (_cursor.isNull(_cursorIndexOfUserId)) {
              _tmpUserId = null;
            } else {
              _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            }
            _result.setUserId(_tmpUserId);
            final String _tmpMealType;
            if (_cursor.isNull(_cursorIndexOfMealType)) {
              _tmpMealType = null;
            } else {
              _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
            }
            _result.setMealType(_tmpMealType);
            final String _tmpFoodType;
            if (_cursor.isNull(_cursorIndexOfFoodType)) {
              _tmpFoodType = null;
            } else {
              _tmpFoodType = _cursor.getString(_cursorIndexOfFoodType);
            }
            _result.setFoodType(_tmpFoodType);
            final String _tmpPhoto_url;
            if (_cursor.isNull(_cursorIndexOfPhotoUrl)) {
              _tmpPhoto_url = null;
            } else {
              _tmpPhoto_url = _cursor.getString(_cursorIndexOfPhotoUrl);
            }
            _result.setPhoto_url(_tmpPhoto_url);
            final boolean _tmpIsLiked;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsLiked);
            _tmpIsLiked = _tmp_2 != 0;
            _result.setLiked(_tmpIsLiked);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public List<RecipeEntity> getRecipesByMealType(final String mealType) {
    final String _sql = "SELECT * FROM recipes WHERE mealType = ? ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (mealType == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, mealType);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
      final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
      final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
      final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
      final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
      final int _cursorIndexOfFoodType = CursorUtil.getColumnIndexOrThrow(_cursor, "foodType");
      final int _cursorIndexOfPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_url");
      final int _cursorIndexOfIsLiked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLiked");
      final List<RecipeEntity> _result = new ArrayList<RecipeEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final RecipeEntity _item;
        _item = new RecipeEntity();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final String _tmpTitle;
        if (_cursor.isNull(_cursorIndexOfTitle)) {
          _tmpTitle = null;
        } else {
          _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
        }
        _item.setTitle(_tmpTitle);
        final List<Ingredient> _tmpIngredients;
        final String _tmp;
        if (_cursor.isNull(_cursorIndexOfIngredients)) {
          _tmp = null;
        } else {
          _tmp = _cursor.getString(_cursorIndexOfIngredients);
        }
        _tmpIngredients = DataConverters.toIngredientList(_tmp);
        _item.setIngredients(_tmpIngredients);
        final List<Step> _tmpInstructions;
        final String _tmp_1;
        if (_cursor.isNull(_cursorIndexOfInstructions)) {
          _tmp_1 = null;
        } else {
          _tmp_1 = _cursor.getString(_cursorIndexOfInstructions);
        }
        _tmpInstructions = DataConverters.toStepList(_tmp_1);
        _item.setInstructions(_tmpInstructions);
        final String _tmpCreated_at;
        if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
          _tmpCreated_at = null;
        } else {
          _tmpCreated_at = _cursor.getString(_cursorIndexOfCreatedAt);
        }
        _item.setCreated_at(_tmpCreated_at);
        final String _tmpUserId;
        if (_cursor.isNull(_cursorIndexOfUserId)) {
          _tmpUserId = null;
        } else {
          _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
        }
        _item.setUserId(_tmpUserId);
        final String _tmpMealType;
        if (_cursor.isNull(_cursorIndexOfMealType)) {
          _tmpMealType = null;
        } else {
          _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
        }
        _item.setMealType(_tmpMealType);
        final String _tmpFoodType;
        if (_cursor.isNull(_cursorIndexOfFoodType)) {
          _tmpFoodType = null;
        } else {
          _tmpFoodType = _cursor.getString(_cursorIndexOfFoodType);
        }
        _item.setFoodType(_tmpFoodType);
        final String _tmpPhoto_url;
        if (_cursor.isNull(_cursorIndexOfPhotoUrl)) {
          _tmpPhoto_url = null;
        } else {
          _tmpPhoto_url = _cursor.getString(_cursorIndexOfPhotoUrl);
        }
        _item.setPhoto_url(_tmpPhoto_url);
        final boolean _tmpIsLiked;
        final int _tmp_2;
        _tmp_2 = _cursor.getInt(_cursorIndexOfIsLiked);
        _tmpIsLiked = _tmp_2 != 0;
        _item.setLiked(_tmpIsLiked);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<RecipeEntity> getRecipesByFoodType(final String foodType) {
    final String _sql = "SELECT * FROM recipes WHERE foodType = ? ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (foodType == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, foodType);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
      final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
      final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
      final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
      final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
      final int _cursorIndexOfFoodType = CursorUtil.getColumnIndexOrThrow(_cursor, "foodType");
      final int _cursorIndexOfPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_url");
      final int _cursorIndexOfIsLiked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLiked");
      final List<RecipeEntity> _result = new ArrayList<RecipeEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final RecipeEntity _item;
        _item = new RecipeEntity();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final String _tmpTitle;
        if (_cursor.isNull(_cursorIndexOfTitle)) {
          _tmpTitle = null;
        } else {
          _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
        }
        _item.setTitle(_tmpTitle);
        final List<Ingredient> _tmpIngredients;
        final String _tmp;
        if (_cursor.isNull(_cursorIndexOfIngredients)) {
          _tmp = null;
        } else {
          _tmp = _cursor.getString(_cursorIndexOfIngredients);
        }
        _tmpIngredients = DataConverters.toIngredientList(_tmp);
        _item.setIngredients(_tmpIngredients);
        final List<Step> _tmpInstructions;
        final String _tmp_1;
        if (_cursor.isNull(_cursorIndexOfInstructions)) {
          _tmp_1 = null;
        } else {
          _tmp_1 = _cursor.getString(_cursorIndexOfInstructions);
        }
        _tmpInstructions = DataConverters.toStepList(_tmp_1);
        _item.setInstructions(_tmpInstructions);
        final String _tmpCreated_at;
        if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
          _tmpCreated_at = null;
        } else {
          _tmpCreated_at = _cursor.getString(_cursorIndexOfCreatedAt);
        }
        _item.setCreated_at(_tmpCreated_at);
        final String _tmpUserId;
        if (_cursor.isNull(_cursorIndexOfUserId)) {
          _tmpUserId = null;
        } else {
          _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
        }
        _item.setUserId(_tmpUserId);
        final String _tmpMealType;
        if (_cursor.isNull(_cursorIndexOfMealType)) {
          _tmpMealType = null;
        } else {
          _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
        }
        _item.setMealType(_tmpMealType);
        final String _tmpFoodType;
        if (_cursor.isNull(_cursorIndexOfFoodType)) {
          _tmpFoodType = null;
        } else {
          _tmpFoodType = _cursor.getString(_cursorIndexOfFoodType);
        }
        _item.setFoodType(_tmpFoodType);
        final String _tmpPhoto_url;
        if (_cursor.isNull(_cursorIndexOfPhotoUrl)) {
          _tmpPhoto_url = null;
        } else {
          _tmpPhoto_url = _cursor.getString(_cursorIndexOfPhotoUrl);
        }
        _item.setPhoto_url(_tmpPhoto_url);
        final boolean _tmpIsLiked;
        final int _tmp_2;
        _tmp_2 = _cursor.getInt(_cursorIndexOfIsLiked);
        _tmpIsLiked = _tmp_2 != 0;
        _item.setLiked(_tmpIsLiked);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<RecipeEntity> getAllRecipesSync() {
    final String _sql = "SELECT * FROM recipes ORDER BY title ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
      final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
      final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
      final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
      final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
      final int _cursorIndexOfFoodType = CursorUtil.getColumnIndexOrThrow(_cursor, "foodType");
      final int _cursorIndexOfPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_url");
      final int _cursorIndexOfIsLiked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLiked");
      final List<RecipeEntity> _result = new ArrayList<RecipeEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final RecipeEntity _item;
        _item = new RecipeEntity();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final String _tmpTitle;
        if (_cursor.isNull(_cursorIndexOfTitle)) {
          _tmpTitle = null;
        } else {
          _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
        }
        _item.setTitle(_tmpTitle);
        final List<Ingredient> _tmpIngredients;
        final String _tmp;
        if (_cursor.isNull(_cursorIndexOfIngredients)) {
          _tmp = null;
        } else {
          _tmp = _cursor.getString(_cursorIndexOfIngredients);
        }
        _tmpIngredients = DataConverters.toIngredientList(_tmp);
        _item.setIngredients(_tmpIngredients);
        final List<Step> _tmpInstructions;
        final String _tmp_1;
        if (_cursor.isNull(_cursorIndexOfInstructions)) {
          _tmp_1 = null;
        } else {
          _tmp_1 = _cursor.getString(_cursorIndexOfInstructions);
        }
        _tmpInstructions = DataConverters.toStepList(_tmp_1);
        _item.setInstructions(_tmpInstructions);
        final String _tmpCreated_at;
        if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
          _tmpCreated_at = null;
        } else {
          _tmpCreated_at = _cursor.getString(_cursorIndexOfCreatedAt);
        }
        _item.setCreated_at(_tmpCreated_at);
        final String _tmpUserId;
        if (_cursor.isNull(_cursorIndexOfUserId)) {
          _tmpUserId = null;
        } else {
          _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
        }
        _item.setUserId(_tmpUserId);
        final String _tmpMealType;
        if (_cursor.isNull(_cursorIndexOfMealType)) {
          _tmpMealType = null;
        } else {
          _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
        }
        _item.setMealType(_tmpMealType);
        final String _tmpFoodType;
        if (_cursor.isNull(_cursorIndexOfFoodType)) {
          _tmpFoodType = null;
        } else {
          _tmpFoodType = _cursor.getString(_cursorIndexOfFoodType);
        }
        _item.setFoodType(_tmpFoodType);
        final String _tmpPhoto_url;
        if (_cursor.isNull(_cursorIndexOfPhotoUrl)) {
          _tmpPhoto_url = null;
        } else {
          _tmpPhoto_url = _cursor.getString(_cursorIndexOfPhotoUrl);
        }
        _item.setPhoto_url(_tmpPhoto_url);
        final boolean _tmpIsLiked;
        final int _tmp_2;
        _tmp_2 = _cursor.getInt(_cursorIndexOfIsLiked);
        _tmpIsLiked = _tmp_2 != 0;
        _item.setLiked(_tmpIsLiked);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
