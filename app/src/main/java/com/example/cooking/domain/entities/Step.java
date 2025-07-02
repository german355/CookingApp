package com.example.cooking.domain.entities;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

/**
 * Представляет один шаг в инструкции по приготовлению рецепта.
 * Содержит номер шага, текстовое описание инструкции и опциональный URL изображения/видео для шага.
 * Реализует Parcelable для передачи между компонентами Android.
 */
public class Step implements Parcelable {
    private int number;
    private String instruction;
    private String url;

    /**
     * Конструктор по умолчанию.
     * Может быть необходим для некоторых библиотек или процессов десериализации.
     */
    public Step() {}

    // Конструктор для Parcelable
    protected Step(Parcel in) {
        number = in.readInt();
        instruction = in.readString();
        url = in.readString();
    }

    // CREATOR для Parcelable
    public static final Creator<Step> CREATOR = new Creator<Step>() {
        @Override
        public Step createFromParcel(Parcel in) {
            return new Step(in);
        }

        @Override
        public Step[] newArray(int size) {
            return new Step[size];
        }
    };

    // Геттеры и сеттеры
    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    // Реализация Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(number);
        dest.writeString(instruction);
        dest.writeString(url);
    }

    // Методы equals() и hashCode() для корректного сравнения и использования в коллекциях.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Step step = (Step) o;
        return number == step.number &&
               Objects.equals(instruction, step.instruction) &&
               Objects.equals(url, step.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, instruction, url);
    }

    // Метод toString() для удобства логирования и отладки.
    @Override
    public String toString() {
        return "Step{" +
                "number=" + number +
                ", instruction='" + instruction + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
