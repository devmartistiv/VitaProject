package com.martist.vitamove.nutrition.ui.model;

import com.martist.vitamove.nutrition.domain.PersonalizedNormsCalculator;

import java.util.function.Function;
import java.util.function.Supplier;


public enum NutrientType {

    VITAMIN_A(900f, "мкг", "Витамин A", Food::getVitaminA, PersonalizedNormsCalculator::getVitaminANorm, Group.VITAMIN),
    VITAMIN_B1(1.2f, "мг", "Витамин B1", Food::getVitaminB1, PersonalizedNormsCalculator::getVitaminB1Norm, Group.VITAMIN),
    VITAMIN_B2(1.3f, "мг", "Витамин B2", Food::getVitaminB2, PersonalizedNormsCalculator::getVitaminB2Norm, Group.VITAMIN),
    VITAMIN_B3(16f, "мг", "Витамин B3", Food::getVitaminB3, PersonalizedNormsCalculator::getVitaminB3Norm, Group.VITAMIN),
    VITAMIN_B5(5f, "мг", "Витамин B5", Food::getVitaminB5, PersonalizedNormsCalculator::getVitaminB5Norm, Group.VITAMIN),

    VITAMIN_B6(1.7f, "мг", "Витамин B6", Food::getVitaminB6, PersonalizedNormsCalculator::getVitaminB6Norm, Group.VITAMIN),
    VITAMIN_B9(400f, "мкг", "Витамин B9", Food::getVitaminB9, PersonalizedNormsCalculator::getVitaminB9Norm, Group.VITAMIN),

    VITAMIN_B12(2.4f, "мкг", "Витамин B12", Food::getVitaminB12, PersonalizedNormsCalculator::getVitaminB12Norm, Group.VITAMIN),
    VITAMIN_C(90f, "мг", "Витамин C", Food::getVitaminC, PersonalizedNormsCalculator::getVitaminCNorm, Group.VITAMIN),
    VITAMIN_D(15f, "мкг", "Витамин D", Food::getVitaminD, PersonalizedNormsCalculator::getVitaminDNorm, Group.VITAMIN),
    VITAMIN_E(15f, "мг", "Витамин E", Food::getVitaminE, PersonalizedNormsCalculator::getVitaminENorm, Group.VITAMIN),
    VITAMIN_K(120f, "мкг", "Витамин K", Food::getVitaminK, PersonalizedNormsCalculator::getVitaminKNorm, Group.VITAMIN),


    CALCIUM(1000f, "мг", "Кальций", Food::getCalcium, PersonalizedNormsCalculator::getCalciumNorm, Group.MINERAL),
    IRON(18f, "мг", "Железо", Food::getIron, PersonalizedNormsCalculator::getIronNorm, Group.MINERAL),
    MAGNESIUM(400f, "мг", "Магний", Food::getMagnesium, PersonalizedNormsCalculator::getMagnesiumNorm, Group.MINERAL),
    PHOSPHORUS(700f, "мг", "Фосфор", Food::getPhosphorus, PersonalizedNormsCalculator::getPhosphorusNorm, Group.MINERAL),
    POTASSIUM(4700f, "мг", "Калий", Food::getPotassium, PersonalizedNormsCalculator::getPotassiumNorm, Group.MINERAL),
    SODIUM(2300f, "мг", "Натрий", Food::getSodium, PersonalizedNormsCalculator::getSodiumNorm, Group.MINERAL),
    ZINC(11f, "мг", "Цинк", Food::getZinc, PersonalizedNormsCalculator::getZincNorm, Group.MINERAL),


    FIBER(25f, "г", "Клетчатка", Food::getFiber, PersonalizedNormsCalculator::getFiberNorm, Group.OTHER),
    SUGAR(50f, "г", "Сахар", Food::getSugar, PersonalizedNormsCalculator::getSugarNorm, Group.OTHER),
    CHOLESTEROL(300f, "мг", "Холестерин", Food::getCholesterol, PersonalizedNormsCalculator::getCholesterolNorm, Group.OTHER),
    SATURATED_FATS(20f, "г", "Насыщенные жиры", Food::getSaturatedFats,
            PersonalizedNormsCalculator::getSaturatedFatsNorm, Group.OTHER),
    TRANS_FATS(2f, "г", "Трансжиры", Food::getTransFats, PersonalizedNormsCalculator::getTransFatsNorm, Group.OTHER);


    public enum Group {
        VITAMIN, MINERAL, OTHER
    }

    private final float defaultNorm;
    private final String unit;
    private final String localizedName;
    final Function<Food, Float> extractor;
    private final Supplier<Float> personalizedNormSupplier;
    private final Group group;

    NutrientType(float defaultNorm, String unit, String localizedName, Function<Food, Float> extractor) {
        this(defaultNorm, unit, localizedName, extractor, null, Group.OTHER);
    }

    NutrientType(float defaultNorm, String unit, String localizedName, Function<Food, Float> extractor,
                 Supplier<Float> personalizedNormSupplier, Group group) {
        this.defaultNorm = defaultNorm;
        this.unit = unit;
        this.localizedName = localizedName;
        this.extractor = extractor;
        this.personalizedNormSupplier = personalizedNormSupplier;
        this.group = group;
    }

    public float getProductValue(Food food) {
        return extractor.apply(food);
    }


    public float getPersonalizedNorm() {
        if (personalizedNormSupplier != null) {
            return personalizedNormSupplier.get();
        }
        return defaultNorm;
    }

    public String getUnit() {
        return unit;
    }

    public String getLocalizedName() {
        return localizedName;
    }

    public Group getGroup() {
        return group;
    }

}
