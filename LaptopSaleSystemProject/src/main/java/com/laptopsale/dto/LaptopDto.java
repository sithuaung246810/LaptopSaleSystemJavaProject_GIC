package com.laptopsale.dto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LaptopDto {
	private Integer id;
	
	@NotEmpty(message = "Model No is required.")
	private String modelNo;
	
//	private String serialNo;	

	private String imageName;
//	private String description;
	@NotNull(message = "Price is required.")
	private Integer price;
	@NotNull(message = "Stock is required.")
	private Integer stock;
	@NotEmpty(message = "Specification is required.")
	private String specification;
	
	@NotEmpty(message = "Model Name is required.")
	private String laptopName;
	
	
	private Integer brandId;
}
