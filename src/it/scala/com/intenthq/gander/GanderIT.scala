package com.intenthq.gander

import java.net.URL
import java.nio.charset.Charset

import com.google.common.base.Charsets
import com.google.common.io.Resources
import com.intenthq.gander.opengraph.OpenGraphData
import org.joda.time.DateTime
import org.specs2.mutable.Specification

class GanderIT extends Specification {

  def extract(url: String, charset: Charset = Charsets.UTF_8): PageInfo = {
    val rawHTML = Resources.toString(new URL(url), charset)
    Gander.extract(rawHTML).get
  }

  def check(pageInfo: PageInfo, title: String, metaDescription: String,
            metaKeywords: String, date: Option[String], content: String, url: String) = {
    pageInfo.title must_== title
    pageInfo.metaDescription must_== metaDescription
    pageInfo.metaKeywords must_== metaKeywords
    pageInfo.publishDate must_== date.map(DateTime.parse(_).toDate)
    pageInfo.cleanedText.get must startWith(content)
    pageInfo.canonicalLink.map( _ must_== url).getOrElse(1 must_== 1)
  }

  "bbc" >> {
    val url = "http://www.bbc.com/news/business-33697945"
    val content = "Disneyland Paris is facing a pricing probe following accusations that UK and German customers are being frozen out of certain price promotions."
    val title = "Disneyland Paris faces pricing probe"
    val metaDescription = "Disneyland Paris is facing a pricing probe following accusations that UK and German customers are being frozen out of promotions available in other European member states."
    val metaKeywords = ""

    check(extract(url), title, metaDescription, metaKeywords, None, content, url)
  }

  "elpais" >> {
    val url = "http://internacional.elpais.com/internacional/2015/07/28/actualidad/1438076596_960360.html"
    val content = "Los aliados de la OTAN ofrecieron este martes respaldo político a Turquía en su ofensiva contra el Estado Islámico tras una reunión convocada de urgencia por el Gobierno de Ankara."
    val title = "La OTAN apoya con cautela la ofensiva turca contra el yihadismo"
    val metaDescription = "La Alianza se ha reunido este martes con carácter de urgencia a pedición de Ankara para tratar el avance del Estado Islámico"
    val metaKeywords = "otan, apoyar, cautela, ofensiva, turca, turco, yihadismo, alianza, haber, reunir, martes, urgencia, pedición, ankara, secretario, general, jens stoltenberg, resaltar, unidad, aliado"

    check(extract(url), title, metaDescription, metaKeywords, Some("2015-07-29"), content, url)
  }

  "corriere" >> {
    val url = "http://www.corriere.it/cronache/15_luglio_29/relazione-alfano-mafia-fatti-gravi-sindaco-ha-sottovalutato-25146a6c-35b0-11e5-b050-7dc71ce7db4c.shtml"
    val content = "ROMA La strada è tracciata, la relazione potrebbe arrivare a Palazzo Chigi prima della pausa estiva. Il ministro dell’Interno Angelino Alfano non proporrà lo scioglimento per mafia del comune di Roma, ma nella relazione al governo"
    val title = "La relazione di Alfano sulla mafia: fatti gravi, il sindaco ha sottovalutato"
    val metaDescription = "Non si propone lo scioglimento ma si lascia aperta la possibilità di una «diversa valutazione»"
    val metaKeywords = "Ignazio Marino, Angelino Alfano"

    check(extract(url, Charsets.ISO_8859_1), title, metaDescription, metaKeywords, None, content, url)
  }

  "lemonde" >> {
    val url = "http://www.lemonde.fr/football/article/2015/07/23/pep-guardiola-un-as-dans-la-manche-des-independantistes_4695701_1616938.html"
    val content = "Dans la planète Barça, Pep Guardiola est un demi-dieu. Entraîneur du FC Barcelone entre 2008 et 2012, il a fait remporter aux Blaugrana 14 titres officiels. Dont six en une seule année : 2009"
    val title = "En Catalogne, Pep Guardiola, figure du Barça, se présente sur la liste indépendantiste"
    val metaDescription = "L’ancien entraîneur du FC Barcelone devrait clore la liste unitaire visant à exiger l’indépendance de la Catalogne lors des élections du 27 septembre."
    val metaKeywords = ""

//    check(extract(url), title, metaDescription, metaKeywords, Some("2015-07-23"), content, url)
    pending
  }

  "folha" >> {
    val url = "http://www1.folha.uol.com.br/esporte/2012/04/1070420-leao-critica-regulamento-do-paulista-e-poe-culpa-na-tv.shtml"
    val canonical = "http://www1.folha.uol.com.br/esporte/1070420-leao-critica-regulamento-do-paulista-e-poe-culpa-na-tv.shtml"
    val content = "Após retomar a liderança do Campeonato Paulista, com a vitória do São Paulo de virada por 4 a 2 sobre o Ituano"
    val title = "Leão critica regulamento do Paulista e põe culpa na TV"
    val metaDescription = "Após retomar a liderança do Campeonato Paulista, com a vitória do São Paulo de virada por 4 a 2 sobre o Ituano, o técnico Emerson Leão voltou a criticar a fórmula de disputa da competição e a FPF (Federação Paulista de Futebol), apontado a culpa para a emissora de televisão dona dos direitos de transmissão."
    val metaKeywords = "São Paulo, Emerson Leão, Campeonato Paulista, FPF,, jornalismo, informação, economia, política, fotografia, imagem, noticiário, cultura, tecnologia, esporte, Brasil, internacional, geral, polícia, manchetes, loteria, loterias, resultados, opinião, análise, cobertura"

    check(extract(url, Charsets.ISO_8859_1), title, metaDescription, metaKeywords, None, content, canonical)
  }

  "lancenet" >> {
    val url = "http://www.lancenet.com.br/sao-paulo/Leao-Arena-Barueri-casa-Tricolor_0_675532605.html"
    val content = "No próximo sábado, o São Paulo jogará, como mandante, na Arena Barueri diante do Mogi Mirim"
    val title = "Para Leão, Arena Barueri não é casa do Tricolor - São Paulo"
    val metaDescription = "No próximo sábado, o São Paulo jogará, como mandante, na Arena Barueri diante do Mogi Mirim. Isso porque no estádio do Morumbi haverá, nesta ..."
    val metaKeywords = "Leao,Arena,Barueri,casa,Tricolor"

    check(extract(url), title, metaDescription, metaKeywords, Some("2012-04-03T18:30:00Z"), content, url)
  }

  "globoesporte" >> {
    val url         = "http://globoesporte.globo.com/futebol/times/sao-paulo/noticia/2012/04/filho-do-gramado-leao-administra-o-sao-paulo-na-base-da-conversa.html"
    val content     = "Emerson Leão não foi ao campo na manhã desta terça-feira no centro de treinamento do São Paulo"
    val title       = "'Filho do gramado', Leão administra o São Paulo na base da conversa"
    val metaDescription = "Emerson Le&atilde;o cobra lideran&ccedil;a ao S&atilde;o Paulo (Foto: M&aacute;rio &Acirc;ngelo / Ag. Estado) Emerson Le&atilde;o n&atilde;o foi ao campo na manh&atilde; desta ter&ccedil;a-feira no centro de treinamento do S&atilde;o Paulo. Bem humorado e com roupa casual, preferiu acompanhar de longe ..."
    val metaKeywords = "notícias, notícia, são paulo"

    check(extract(url), title, metaDescription, metaKeywords, Some("2012-04-01"), content, url)
  }

  "opengraph" >> {
    val url = "http://internacional.elpais.com/internacional/2015/07/28/actualidad/1438076596_960360.html"

    val pageInfo = extract(url)
    pageInfo.openGraphData must_== OpenGraphData(title = Some("La OTAN apoya con cautela la ofensiva turca contra el yihadismo"),
                                                 siteName = Some("EL PAÍS"),
                                                 url = Some(new URL(url)),
                                                 description = Some("La Alianza se ha reunido este martes con carácter de urgencia a pedición de Ankara para tratar el avance del Estado Islámico"),
                                                 image = Some(new URL("http://ep00.epimg.net/internacional/imagenes/2015/07/28/actualidad/1438076596_960360_1438078067_noticia_normal.jpg")),
                                                 `type` = Some("article"),
                                                 locale = None,
                                                 publishedTime = Some(new DateTime(2015, 7, 29, 0, 0)))

  }

}

