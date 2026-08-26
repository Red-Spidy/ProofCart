package com.proofcart.catalog.openfoodfacts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsResponse {

    private List<ProductData> products;

    public List<ProductData> getProducts() {
        return products;
    }

    public void setProducts(List<ProductData> products) {
        this.products = products;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductData {
        private String code; // Barcode
        
        @JsonProperty("product_name")
        private String productName;
        
        private String brands;
        
        @JsonProperty("image_url")
        private String imageUrl;
        
        @JsonProperty("ingredients_text")
        private String ingredientsText;
        
        @JsonProperty("allergens_tags")
        private List<String> allergensTags; // e.g. ["en:milk", "en:nuts"]
        
        @JsonProperty("labels_tags")
        private List<String> labelsTags; // e.g. ["en:vegan", "en:vegetarian"]

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getBrands() {
            return brands;
        }

        public void setBrands(String brands) {
            this.brands = brands;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getIngredientsText() {
            return ingredientsText;
        }

        public void setIngredientsText(String ingredientsText) {
            this.ingredientsText = ingredientsText;
        }

        public List<String> getAllergensTags() {
            return allergensTags;
        }

        public void setAllergensTags(List<String> allergensTags) {
            this.allergensTags = allergensTags;
        }

        public List<String> getLabelsTags() {
            return labelsTags;
        }

        public void setLabelsTags(List<String> labelsTags) {
            this.labelsTags = labelsTags;
        }
    }
}
