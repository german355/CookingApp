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
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class LikedRecipeDao_Impl implements LikedRecipeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LikedRecipeEntity> __insertionAdapterOfLikedRecipeEntity;

  private final EntityDeletionOrUpdateAdapter<LikedRecipeEntity> __deletionAdapterOfLikedRecipeEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public LikedRecipeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLikedRecipeEntity = new EntityInsertionAdapter<LikedRecipeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `liked_recipes` (`recipeId`) VALUES (?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final LikedRecipeEntity entity) {
        statement.bindLong(1, entity.getRecipeId());
      }
    };
    this.__deletionAdapterOfLikedRecipeEntity = new EntityDeletionOrUpdateAdapter<LikedRecipeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `liked_recipes` WHERE `recipeId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final LikedRecipeEntity entity) {
        statement.bindLong(1, entity.getRecipeId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM liked_recipes WHERE recipeId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM liked_recipes";
        return _query;
      }
    };
  }

  @Override
  public void insert(final LikedRecipeEntity likedRecipe) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfLikedRecipeEntity.insert(likedRecipe);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertAll(final List<LikedRecipeEntity> likedRecipes) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfLikedRecipeEntity.insert(likedRecipes);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final LikedRecipeEntity likedRecipe) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfLikedRecipeEntity.handle(likedRecipe);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
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
  public LiveData<List<LikedRecipeEntity>> getLikedRecipes() {
    final String _sql = "SELECT * FROM liked_recipes";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"liked_recipes"}, false, new Callable<List<LikedRecipeEntity>>() {
      @Override
      @Nullable
      public List<LikedRecipeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRecipeId = CursorUtil.getColumnIndexOrThrow(_cursor, "recipeId");
          final List<LikedRecipeEntity> _result = new ArrayList<LikedRecipeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LikedRecipeEntity _item;
            final int _tmpRecipeId;
            _tmpRecipeId = _cursor.getInt(_cursorIndexOfRecipeId);
            _item = new LikedRecipeEntity(_tmpRecipeId);
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
  public boolean isRecipeLiked(final int recipeId) {
    final String _sql = "SELECT EXISTS(SELECT 1 FROM liked_recipes WHERE recipeId = ? LIMIT 1)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, recipeId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final boolean _result;
      if (_cursor.moveToFirst()) {
        final int _tmp;
        _tmp = _cursor.getInt(0);
        _result = _tmp != 0;
      } else {
        _result = false;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<Integer> getLikedRecipeIdsSync() {
    final String _sql = "SELECT recipeId FROM liked_recipes";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final List<Integer> _result = new ArrayList<Integer>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Integer _item;
        if (_cursor.isNull(0)) {
          _item = null;
        } else {
          _item = _cursor.getInt(0);
        }
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
