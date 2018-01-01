package gotwifi

import org.springframework.data.domain.Example
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.Embeddable
import javax.persistence.Table
import javax.persistence.Column
import javax.persistence.ManyToOne
import javax.persistence.ManyToMany
import javax.persistence.UniqueConstraint
import org.springframework.beans.factory.annotation.Autowired
import com.abstratt.kirra.spring.*

@Entity
data class Contributor(override var id: Long? = null, @Column(unique = true) var name: String? = null)
    : BaseEntity(id)

@Service
class ContributorService : BaseService<Contributor>()

@Entity
data class Place(
    override var id: Long? = null,
    @Column(unique = true) var name: String? = null,
    var address: Address? = null,
    @ManyToOne var placeType: PlaceType? = null
) : BaseEntity(id)

@Service
class PlaceService : BaseService<Place>()

@Embeddable
data class Address(
        var streetAddress: String?,
        var city: String?,
        var state: String?,
        var country: String?
)

@Entity
@Table(uniqueConstraints = arrayOf(UniqueConstraint(name = "one_review_per_contributor", columnNames = arrayOf("contributor_id", "place_id"))))
data class Review(
    override var id: Long? = null,
    @ManyToOne var contributor: Contributor? = null,
    @ManyToOne var place: Place? = null,
    var overallRating: Rating? = null,
    var foodRating: Rating? = null,
    var serviceRating: Rating? = null,
    var priceRating: Rating? = null,
    var comfortRating: Rating? = null,
    var noiseRating: Rating? = null,
    var internetRating: Rating? = null,
    var wifiPassword: String? = null,
    var wifiPasswordRequired: Boolean? = null,
    @ManyToMany var foods: Collection<Food> = emptySet(),
    @ManyToMany var drinks: Collection<Drink> = emptySet(),
    @ManyToMany var seats: Collection<Seat> = emptySet()
) : BaseEntity(id) {
    var date: LocalDateTime? = null
}
@Service
class ReviewService : BaseService<Review>()  {
    @Autowired
    lateinit var placeRepository: PlaceRepository
    @Autowired
    lateinit var contributorRepository: ContributorRepository

    fun reviewsFor(place: Place): Page<Review> {
        return repository.findAll(Example.of(Review(place = place)), defaultPageRequest())
    }

    fun reviewsBy(contributor: Contributor): Page<Review> {
        return repository.findAll(Example.of(Review(contributor = contributor)), defaultPageRequest())
    }

    override fun create(toCreate: Review): Review {
        toCreate.place = placeRepository.findOne(Example.of(toCreate.place))
        toCreate.contributor = contributorRepository.findOne(Example.of(toCreate.contributor))
        toCreate.date = LocalDateTime.now()
        return super.create(toCreate)
    }

    override fun update(toUpdate: Review): Review? {
        toUpdate.date = LocalDateTime.now()
        return super.update(toUpdate)
    }
}

@Entity
data class Food(override var id: Long? = null,
                var name: String) : BaseEntity(id)

@Entity
data class Drink(
        override var id: Long? = null,
        var name: String
) : BaseEntity(id)

@Entity
data class Seat(override var id: Long? = null,
                var name: String) : BaseEntity(id)

@Entity
data class PlaceType(override var id: Long? = null,
                     var name: String) : BaseEntity(id)

enum class Rating(val level: Int) {
    NotApplicable(0),
    Terrible(1),
    Bad(2),
    Regular(3),
    Good(4),
    Excellent(5)
}


interface ContributorRepository : BaseRepository<Contributor>

interface ReviewRepository : BaseRepository<Review>

interface PlaceRepository : BaseRepository<Place>
