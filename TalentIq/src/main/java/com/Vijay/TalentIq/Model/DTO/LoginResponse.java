package com.Vijay.TalentIq.Model.DTO;

public record LoginResponse
                    (String message, 
                    		String firstname,
                    		String lastname, 
                    		String email)
{}