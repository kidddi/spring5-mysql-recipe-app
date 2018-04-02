package guru.springframework.services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import guru.springframework.commands.IngredientCommand;
import guru.springframework.converters.IngredientCommandToIngredient;
import guru.springframework.converters.IngredientToIngredientCommand;
import guru.springframework.domain.Ingredient;
import guru.springframework.domain.Recipe;
import guru.springframework.exceptions.NotFoundException;
import guru.springframework.repositories.RecipeRepository;
import guru.springframework.repositories.UnitOfMeasureRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by jt on 6/28/17.
 */
@Slf4j
@Service
public class IngredientServiceImpl implements IngredientService {

	private final IngredientToIngredientCommand ingredientToIngredientCommand;
	private final IngredientCommandToIngredient ingredientCommandToIngredient;
	private final RecipeRepository recipeRepository;
	private final UnitOfMeasureRepository unitOfMeasureRepository;

	public IngredientServiceImpl(IngredientToIngredientCommand ingredientToIngredientCommand,
			IngredientCommandToIngredient ingredientCommandToIngredient, RecipeRepository recipeRepository,
			UnitOfMeasureRepository unitOfMeasureRepository) {
		this.ingredientToIngredientCommand = ingredientToIngredientCommand;
		this.ingredientCommandToIngredient = ingredientCommandToIngredient;
		this.recipeRepository = recipeRepository;
		this.unitOfMeasureRepository = unitOfMeasureRepository;
	}

	@Override
	public IngredientCommand findByRecipeIdAndIngredientId(Long recipeId, Long ingredientId) {

		Recipe recipe = recipeRepository.findById(recipeId)
				.orElseThrow(() -> new NotFoundException("recipe id not found. Id: " + recipeId));

		return recipe.getIngredients()
				.stream()
				.filter(ingredient -> ingredient.getId().equals(ingredientId))
				.map(ingredient -> ingredientToIngredientCommand.convert(ingredient))
				.findFirst()
				.orElseThrow(() -> new NotFoundException("Ingredient id not found: " + ingredientId));
	}

	@Override
	@Transactional
	public IngredientCommand saveIngredientCommand(IngredientCommand command) {

		Recipe recipe = recipeRepository.findById(command.getRecipeId())
				.orElseThrow(() -> new NotFoundException("Recipe not found for id: " + command.getRecipeId()));

		Optional<Ingredient> ingredientOptional = recipe.getIngredients()
				.stream()
				.filter(ingredient -> ingredient.getId()
						.equals(command.getId()))
				.findFirst();

		if (ingredientOptional.isPresent()) {
			Ingredient ingredientFound = ingredientOptional.get();
			ingredientFound.setDescription(command.getDescription());
			ingredientFound.setAmount(command.getAmount());
			ingredientFound.setUom(unitOfMeasureRepository.findById(command.getUom().getId())
					.orElseThrow(() -> new RuntimeException("UOM NOT FOUND"))); // todo address this
		} else {
			// add new Ingredient
			Ingredient ingredient = ingredientCommandToIngredient.convert(command);
			ingredient.setRecipe(recipe);
			recipe.addIngredient(ingredient);
		}

		Recipe savedRecipe = recipeRepository.save(recipe);

		Optional<Ingredient> savedIngredientOptional = savedRecipe.getIngredients()
				.stream()
				.filter(recipeIngredients -> recipeIngredients.getId()
						.equals(command.getId()))
				.findFirst();

		// check by description
		if (!savedIngredientOptional.isPresent()) {
			// not totally safe... But best guess
			savedIngredientOptional = savedRecipe.getIngredients()
					.stream()
					.filter(recipeIngredients -> recipeIngredients.getDescription().equals(command.getDescription()))
					.filter(recipeIngredients -> recipeIngredients.getAmount().equals(command.getAmount()))
					.filter(recipeIngredients -> recipeIngredients.getUom().getId().equals(command.getUom().getId()))
					.findFirst();
		}

		// to do check for fail
		return ingredientToIngredientCommand.convert(savedIngredientOptional.get());
	}

	@Override
	public void deleteById(Long recipeId, Long idToDelete) {

		log.debug("Deleting ingredient: " + recipeId + ":" + idToDelete);

		Recipe recipe = recipeRepository.findById(recipeId)
				.orElseThrow(() -> new NotFoundException("Recipe Id Not found. Id:" + recipeId));

		Ingredient ingredientToDelete = recipe.getIngredients()
				.stream()
				.filter(ingredient -> ingredient.getId()
						.equals(idToDelete))
				.findFirst()
				.orElseThrow(() -> new NotFoundException("Ingredient Not found. Id:" + idToDelete));

		ingredientToDelete.setRecipe(null);
		recipe.getIngredients()
				.remove(ingredientToDelete);
		recipeRepository.save(recipe);
	}
}
