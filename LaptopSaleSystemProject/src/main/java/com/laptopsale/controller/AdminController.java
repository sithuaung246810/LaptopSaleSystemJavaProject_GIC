package com.laptopsale.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.laptopsale.Repository.PurchaseDetailRepository;
import com.laptopsale.Repository.UserRepository;
import com.laptopsale.Service.BrandService;
import com.laptopsale.Service.LaptopService;
import com.laptopsale.Service.UserService;
import com.laptopsale.dto.LaptopDto;
import com.laptopsale.entity.Brand;
import com.laptopsale.entity.Laptop;
import com.laptopsale.entity.PurchaseDetail;
import com.laptopsale.entity.User;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;


@Controller
public class AdminController {
public static String uploadDir = System.getProperty("user.dir")+"/public/laptopImages";
	
	@Autowired
	BrandService brandService;
	
	@Autowired
	LaptopService laptopService;
	
	@Autowired
	PurchaseDetailRepository purchaseDetailRepository;
	
	@Autowired
    private UserService userService;
	
	
	private boolean isAdmin(HttpSession session) {
	    String userEmail = (String) session.getAttribute("email");
	    User user = userService.findByEmail(userEmail);
	    
	    
	    if (user != null) {
	        String role = user.getRole();
	        session.setAttribute("adminRole", role);  // Set the adminRole in session
	        return role.equals("ROLE_ADMIN") || role.equals("ROLE_OWNER");
	    }
	    
	    return false;	
	    

	}

	
	@GetMapping("/admin")
	public String addBrands(HttpSession session,Model model) {
		if (!isAdmin(session)) {
	        return "redirect:/login";
	    }
	
		Long ucount , acount , bancount= (long) 0;
		Integer lcount , bcount  = 0;
		Integer pcount;
		 ucount = userService.CountUsers();
		 acount = userService.CountAdmins();
		 bancount = userService.CountBanUser();
		 lcount = laptopService.sumActiveLaptops();
		 bcount = brandService.countActiveBrand();
		 pcount = purchaseDetailRepository.sumAllPurchase();
		
	
		
		model.addAttribute("ucount", ucount );
		model.addAttribute("lcount", lcount);
		model.addAttribute("bancount",  bancount );
		model.addAttribute("bcount", bcount);
		model.addAttribute("pcount", pcount == null ? pcount = 0 : pcount);
		model.addAttribute("acount",  acount );
		session.setAttribute("intendedDestination", "/admin");
		
		
		return "Admin";
		
		
	}
	

	@GetMapping("/admin/brandlist/reverse")
	public String addtestBrandsReverse(Model model,@Param("keyword")String keyword, HttpSession session) {
		if (!isAdmin(session)) {
	        return "redirect:/login";
	    }
//		model.addAttribute("brands", brandService.getAllBrands(keyword).reversed());
		model.addAttribute("brands", brandService.findAllActiveBrands(null, null, keyword, keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("brand", new Brand());
		return "brandAdmin";
	}
	
	@PostMapping("/admin/brand/add")
	public String addBrandsTest(@ModelAttribute("brand") Brand brand, HttpSession session,
			RedirectAttributes redirectAttributes,Model model
           ) {
		if (!isAdmin(session)) {
	        return "redirect:/login"; 
	    }
//		String adminRole = (String) session.getAttribute("adminRole");
		
		 
	    
		 try {
			 brand.setDeleted(false);
			 brandService.addBrand(brand);
			 redirectAttributes.addFlashAttribute("success", "Brand save successfully");
		 }
		 catch(IllegalArgumentException e) {
			 redirectAttributes.addFlashAttribute("error", e.getMessage());
		 }
		 catch (Exception e) {
			// TODO: handle exception
			 redirectAttributes.addFlashAttribute("error", "An error occured while saving the brad. Please try again.");
			 
		}
			
		return"redirect:/admin/brandlist";
	}
	@PostMapping("/admin/brand/update")
	public String updateBrands(HttpSession session, Model model,
	                           @Valid @ModelAttribute("brand") Brand brand,
	                           BindingResult bindingResult,
	                           RedirectAttributes redirectAttributes) {
	    if (!isAdmin(session)) {
	        return "redirect:/login";
	    }
//	    String adminRole = (String) session.getAttribute("adminRole");
	    

	    // Check for form validation errors
	    if (bindingResult.hasErrors()) {
	        return "brandAdminUpdate"; // Return to the form view with errors
	    }

//	    // Check for existing brand name
	    if (brandService.existsByBrandNameAndIdNot(brand.getBrandName(),brand.getId())) {
	        bindingResult.rejectValue("brandName", "error.brand", "Brand Name already exists.");
	  
	        return "brandAdminUpdate";
	    }

	 
	    	
	    try {
	        brandService.updateBrand(brand);
	        redirectAttributes.addFlashAttribute("success", "Brand updated successfully");
	    } catch (IllegalArgumentException e) {
	        redirectAttributes.addFlashAttribute("error", e.getMessage());
	    } catch (Exception e) {
	        redirectAttributes.addFlashAttribute("error", "An error occurred while saving the brand. Please try again.");
	    }

	    return "redirect:/admin/brandlist";
	}
	
	
	
	@GetMapping("/admin/brand/update/{id}")
	public String updateBrandTest(Model model,@Param("keyword")String keyword,@PathVariable("id") int id, HttpSession session) {
		if (!isAdmin(session)) {
	        return "redirect:/login";
	    }
		Optional<Brand> brand =brandService.getBrandById(id);
		if(brand.isPresent()) {
			model.addAttribute("brands", brandService.getAllBrands(keyword));
			model.addAttribute("brand",brand.get());
			return "brandAdminUpdate";
		}else {
			return "404|Page Not Found";
		}
	}
	
	
	
	
	
	//	Laptop
	@GetMapping("/admin/laptops")
	public String showLaptops(Model model, HttpSession session) {
		if (!isAdmin(session)) {
	        return "redirect:/login";
	    }
	
		
		return findPaginated(1, "id","desc",model);
	}
	

	@GetMapping("/admin/laptops/add")
	public String LaptopsAddGet(Model model, HttpSession session) {
		if (!isAdmin(session)) {
	        return "redirect:/login";
	    }
		model.addAttribute("laptopDto", new LaptopDto());
		model.addAttribute("brands", brandService.getAllBrands(null));
		return "LaptopAdd";
	}
	
	
	@PostMapping("/admin/laptops/add")
	public String laptopsAddPost(@Valid @ModelAttribute("laptopDto") LaptopDto laptopDto,
			 BindingResult bindingResult,
	            Model model,
								@RequestParam("laptopImage") MultipartFile file,
								RedirectAttributes redirectAttributes,
								@RequestParam("imgName")String imgName) throws IOException
{
		
		  // Check for validation errors
		    if (bindingResult.hasErrors()) {
		        model.addAttribute("error", "Please fix the errors below and try again.");
				model.addAttribute("brands", brandService.getAllBrands(null));
		        return "LaptopAdd";
		    }
		    
			  // Check for existing brand name
//		    if (laptopService.laptopNameExists(laptopDto.getLaptopName())) {
//		        bindingResult.rejectValue("laptopName", "error.laptop", "laptopName already exists.");
//		    	model.addAttribute("brands", brandService.getAllBrands(null));
//		        return "LaptopAdd";
//		    }
		 // Check for existing brand name
			  if (laptopService.laptopNameExistsForOtherId(laptopDto.getLaptopName(), laptopDto.getId())) {
				    bindingResult.rejectValue("laptopName", "error.laptop", "Laptop name already exists.");
				    model.addAttribute("brands", brandService.getAllBrands(null));
				    return "LaptopUpdate";
				}

		Laptop laptop = new Laptop();
		laptop.setId(laptopDto.getId());
		laptop.setImageName(laptopDto.getImageName());
		laptop.setBrand(brandService.getBrandById(laptopDto.getBrandId()).get());
		laptop.setPrice(laptopDto.getPrice());
//		laptop.setDescription(laptopDto.getDescription());
		laptop.setLaptopName(laptopDto.getLaptopName());
		laptop.setSpecification(laptopDto.getSpecification());
		laptop.setModelNo(laptopDto.getModelNo());
//		laptop.setSerialNo(laptopDto.getSerialNo());s
		laptop.setStock(laptopDto.getStock());
		
		
		  String imageUUID; 
		  if(!file.isEmpty()) 
		  { 
		  imageUUID = file.getOriginalFilename();
		  Path fileNameAndPath = Paths.get(uploadDir, imageUUID); 
		  Files.write(fileNameAndPath, file.getBytes()); 
		  }else { 
			  imageUUID = imgName; 
			  } 
		  
		  try {
			  
			  laptop.setImageName(imageUUID);
			  	laptop.setDeleted(false);
			  	laptopService.addLaptop(laptop);
			  	redirectAttributes.addFlashAttribute("success", "Laptop Added Successfully");
			 }
			 catch(IllegalArgumentException e) {
				 redirectAttributes.addFlashAttribute("error", "Laptop Model Name is already exist. Please try again.");
			 }
			 catch (Exception e) {
				// TODO: handle exception
				 redirectAttributes.addFlashAttribute("error", "Laptop Model Name is already exist. Please try again.");
			}
				
		
		return "redirect:/admin/laptops";
	}
	
	// Here Repair Code and need update in laptop service and repository and html form action................
	@PostMapping("/admin/laptops/update")
	public String laptopUpdatePost(@Valid @ModelAttribute("laptopDto") LaptopDto laptopDto,
			 BindingResult bindingResult,
	            Model model,
								@RequestParam("laptopImage") MultipartFile file,
								RedirectAttributes redirectAttributes,
								@RequestParam("imgName")String imgName) throws IOException
{
		
		  if (bindingResult.hasErrors()) {
		        model.addAttribute("error", "Please fix the errors below and try again.");
				model.addAttribute("brands", brandService.getAllBrands(null));
		        return "LaptopUpdate";
		    }
		  

		// Check for existing brand name
		  if (laptopService.laptopNameExistsForOtherId(laptopDto.getLaptopName(), laptopDto.getId())) {
			    bindingResult.rejectValue("laptopName", "error.laptop", "Laptop name already exists.");
			    model.addAttribute("brands", brandService.getAllBrands(null));
			    return "LaptopUpdate";
			}
		  
		Laptop laptop = new Laptop();
		laptop.setId(laptopDto.getId());
		laptop.setImageName(laptopDto.getImageName());
		laptop.setBrand(brandService.getBrandById(laptopDto.getBrandId()).get());
		laptop.setPrice(laptopDto.getPrice());
		laptop.setLaptopName(laptopDto.getLaptopName());
		laptop.setSpecification(laptopDto.getSpecification());
		laptop.setModelNo(laptopDto.getModelNo());
		laptop.setStock(laptopDto.getStock());
		
		
		  String imageUUID; 
		  if(!file.isEmpty()) 
		  { 
		  imageUUID = file.getOriginalFilename();
		  Path fileNameAndPath = Paths.get(uploadDir, imageUUID); 
		  Files.write(fileNameAndPath, file.getBytes()); 
		  }else { 
			  imageUUID = imgName; 
			  } 
		  
	
		  
		  laptop.setImageName(imageUUID);
		redirectAttributes.addFlashAttribute("success", "Laptop Update Successfully");
		laptopService.updateLaptop(laptop);
		return "redirect:/admin/laptops";
	}
	
	@GetMapping("admin/laptops/update/{id}")
	public String updateLaptop(@PathVariable("id") int id,Model model)
	{
		 Laptop laptop= laptopService.getAdminLaptopById(id).get();
		 LaptopDto laptopDto = new LaptopDto();
		 laptopDto.setId(laptop.getId());
		 laptopDto.setModelNo(laptop.getModelNo());
//		 laptopDto.setSerialNo(laptop.getSerialNo());
		 laptopDto.setImageName(laptop.getImageName());
		 laptopDto.setBrandId(laptop.getBrand().getId());
//		 laptopDto.setDescription(laptop.getDescription());
		 laptopDto.setLaptopName(laptop.getLaptopName());
		
		 laptopDto.setSpecification(laptop.getSpecification());
		 laptopDto.setPrice(laptop.getPrice());
		 laptopDto.setStock(laptop.getStock());
		 
		 model.addAttribute("brands", brandService.getAllBrands(null));
		 model.addAttribute("laptopDto", laptopDto);
		return "LaptopUpdate";
	}
	
	//	Pagination Laptop
	
	@GetMapping("/page/{pageNo}")
	public String findPaginated(@PathVariable( value = "pageNo") int pageNo, 
			 @RequestParam(value = "sortField", defaultValue = "id") String sortField,  // Default to id
		     @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,    // Default to descending order

			Model model) {
		//Model --- Data passs
		
		int pageSize = 4;
		
		Page<Laptop> page = laptopService.findPaginated(pageNo, pageSize,sortField,sortDir);

		List<Laptop> listLaptoppage = page.getContent();
		model.addAttribute("currentPage", pageNo);
		
		model.addAttribute("totalPages", page.getTotalPages());
		model.addAttribute("totalItems", page.getTotalElements());
		
		model.addAttribute("sortField", sortField);
		model.addAttribute("sortDir", sortDir);
		model.addAttribute("reverseSortDir", sortDir.equals("asc")? "desc":"asc");

		model.addAttribute("listLaptops", listLaptoppage);
//		model.addAttribute("listLaptops", laptopService.findAllActiveLaptopList());

		
		return "LaptopList";
		
	}
	
	//Purchase List
	@GetMapping("/admin/laptops/purchaselist")
	public String showPurchaseList(Model model,@Param("keyword")String keyword) {
//		String keyword = "12";
		model.addAttribute("laptops", laptopService.getAllLaptops(keyword));
		return"LaptopList";
	}

	@GetMapping("ahome")
	public String showTest() {
		return "AdminTest";
	}
	@GetMapping("/admin/historys")
	public String showHistorys(
	        @RequestParam(value = "purchaseDate", required = false)
	        @DateTimeFormat(pattern = "yyyy-MM-dd") Date purchaseDate,
	        @RequestParam(value = "page", defaultValue = "0") int page,  // Page number (0-based index)
	        @RequestParam(value = "size", defaultValue = "5") int size, // Page size
	        Model model,
	        HttpSession session) {
 
	    // Check if the user is an admin; if not, redirect to the login page
	    if (!isAdmin(session)) {
	        return "redirect:/login";
	    }
 
	    Pageable pageable = PageRequest.of(page, size);
	    Page<PurchaseDetail> purchaseDetails;
 
	    if (purchaseDate == null) {
	        // Fetch all purchase details with pagination
	        purchaseDetails = purchaseDetailRepository.findAll(pageable);
	    } else {
	        // Fetch purchase details for the selected date with pagination
	        purchaseDetails = purchaseDetailRepository.findByPurchaseDate(purchaseDate, pageable);
	        model.addAttribute("selectedDate", purchaseDate);
	    }
 
	    model.addAttribute("purchaseDetails", purchaseDetails.getContent());
	    model.addAttribute("totalPages", purchaseDetails.getTotalPages());
	    model.addAttribute("currentPage", page);
	    model.addAttribute("totalItems", purchaseDetails.getTotalElements());
 
	    return "PurchaseHistory";
	}

	
	//Soft Delete
	
	@GetMapping("/admin/brandlist")
	public String addTestBrands(Model model, @Param("keyword") String keyword, HttpSession session, 
	                            @RequestParam(defaultValue = "1") int page, 
	                            @RequestParam(defaultValue = "5") int size,  
	                            @RequestParam(value = "sortField", defaultValue = "id") String sortField,  
	                            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {
	    if (!isAdmin(session)) {
	        return "redirect:/login";
	    }

	    Page<Brand> brandPage = brandService.findAllActiveBrands(page, size, sortField, sortDir);
	    model.addAttribute("brands", brandPage.getContent());
	    model.addAttribute("currentPage", page);
	    model.addAttribute("totalPages", brandPage.getTotalPages());
	    model.addAttribute("totalItems", brandPage.getTotalElements());

	    // Adding sorting information
	    model.addAttribute("sortField", sortField);
	    model.addAttribute("sortDir", sortDir);
	    model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

	    model.addAttribute("brand", new Brand());

	    return "brandAdmin";
	}

	

	@GetMapping("/admin/brand/delete/{id}")
	public String softDeleteBrand(@PathVariable Integer id,HttpSession session){
		
		if (!isAdmin(session)) {
	        return "redirect:/login";
	    }
//		brandService.deleteBrandById(id);

		brandService.softDeletBrands(id);
		System.out.println("Delete by Soft");
		return "redirect:/admin/brandlist";
//		return ResponseEntity.noContent().build();
	}
	
	@GetMapping("admin/laptops/delete/{id}")
	public String deleteLaptop(@PathVariable int id) {
//		laptopService.deleteLaptopById(id);
		laptopService.softDeletLaptops(id);
		System.out.println("Delete by Soft Laptop");
		return "redirect:/admin/laptops";
	}
	
	
	///User
	
	@GetMapping("/admin/users")
	public String getUsersByRole(@RequestParam(required = false, defaultValue = "ALL") String role, 
	                             Model model, HttpSession session, 
	                             @RequestParam(defaultValue = "1") int page, 
	                             @RequestParam(defaultValue = "5") int size,  
	                             

	                             @RequestParam(value = "sortField", defaultValue = "id") String sortField,  
	                             @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {

	    if (!isAdmin(session)) {
	        return "redirect:/login";
	    }
	
	    
	    String adminRole = (String) session.getAttribute("adminRole");
	    List<String> roles = userService.getDistinctRoles(); // Fetch distinct roles for dropdown
	    
	    Page<User> userPage;

	    if ("ALL".equals(role)) {
	        userPage = userService.findAllUsersWithPagination(page, size, sortField, sortDir);
	    } else {
	        userPage = userService.findUsersByRoleWithPagination(role, page, size, sortField, sortDir);
	    }

	    // Add attributes to model
	    model.addAttribute("roles", roles);  // Add roles to the model
	    model.addAttribute("selectedRole", role);  // Add selected role to the model
	    model.addAttribute("users", userPage.getContent());  // Add paginated users to the model
	    model.addAttribute("currentPage", page);
	    model.addAttribute("totalPages", userPage.getTotalPages());
	    model.addAttribute("totalItems", userPage.getTotalElements());
	    
	    // Sorting information
	    model.addAttribute("sortField", sortField);
	    model.addAttribute("sortDir", sortDir);
	    model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
	    
	    model.addAttribute("adminRole", adminRole);
	    
	    return "user";
	}

	

//	@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
	@GetMapping("/admin/users/updateRole/{id}/{role}")
	public String updateUserRole(
	    @PathVariable("id") Integer id, 
	    @PathVariable("role") String role,
	    Model model) {

	    User user = userService.findById(id);

	    // Set role only if the provided role is valid
	    if (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("USER") || role.equalsIgnoreCase("BAN")) {
	        user.setRole("ROLE_" + role.toUpperCase());
	        userService.saveUser(user);
	    }
	  

	    // Redirect to the user list after updating the role
	    return "redirect:/admin/users";
	}

	

}
