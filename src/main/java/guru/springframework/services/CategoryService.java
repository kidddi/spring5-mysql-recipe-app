package guru.springframework.services;

import java.util.Set;

import guru.springframework.commands.CategoryCommand;

/**
 * Created by jt on 6/28/17.
 */
public interface CategoryService {

    Set<CategoryCommand> listAllCategoryes();
}
